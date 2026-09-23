# Technical Architecture
**Project:** ¿WhoKnows?: Android Trivia Game
**Context:** Mobile Programming course project, University of Rome Tor Vergata (A.Y. 2023/24)

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Layers](#2-layers)
3. [Navigation](#3-navigation)
4. [ViewModels and UI State](#4-viewmodels-and-ui-state)
5. [Concurrency Model](#5-concurrency-model)
6. [Networking](#6-networking)
7. [Connectivity and Offline Handling](#7-connectivity-and-offline-handling)
8. [Persistence](#8-persistence)
9. [Lifecycle and Audio](#9-lifecycle-and-audio)
10. [Testing and CI](#10-testing-and-ci)
11. [Dependencies](#11-dependencies)

---

## 1. System Overview

¿WhoKnows? is a **single-activity** Android application written in Kotlin. The whole UI is built with **Jetpack Compose** (Material 3) and hosted by `MainActivity`, which owns a `NavHost` with four destinations.

The app follows **MVVM with a Repository layer**:

```
┌──────────────────────────────────────────────────────────────┐
│  UI (Compose)     HomeView · GameView · StatsView · Settings │
└───────────────┬──────────────────────────────▲───────────────┘
                │ events (onClick → VM calls)  │ observeAsState()
┌───────────────▼──────────────────────────────┴───────────────┐
│  ViewModels       GameViewModel · StatsViewModel · Settings  │
│                   (LiveData, viewModelScope)                 │
└───────────────┬──────────────────────────────────────────────┘
                │ suspend calls
┌───────────────▼──────────────────────────────────────────────┐
│  Repositories     QuestionRepository · GameRepository ·      │
│                   GameQuestionRepository                     │
└───────┬───────────────────────────────┬──────────────────────┘
        │ Retrofit (Dispatchers.IO)     │ Room suspend DAOs
┌───────▼────────────┐        ┌─────────▼──────────────────────┐
│ Open Trivia DB API │        │ SQLite: questions, games,      │
│ (opentdb.com)      │        │ games_questions                │
└────────────────────┘        └────────────────────────────────┘

        NetworkMonitorService ── isOffline: LiveData<Boolean> ──► GameView / GameViewModel
```

There is no backend of our own: the only remote dependency is the free, public Open Trivia DB API. All user data stays on the device.

---

## 2. Layers

| Layer | Package | Responsibility |
|---|---|---|
| UI | `ui.screens.views`, `ui.screens.components`, `ui.theme` | Stateless-ish Compose screens that observe ViewModel state and forward user events. Separate portrait and landscape composables where the layout differs. |
| Presentation | `ui.viewmodels` | Game state machine, timers, scoring, audio, and exposure of state as `LiveData`. |
| Domain helpers | `utils` | Pure game rules (`GameRules`), category mapping, Room type converters, the JSON deserializer, and orientation helpers. |
| Data | `repository`, `data.dao`, `data.db`, `data.model`, `data.network` | Access to the REST API and the local database behind a small repository API. Network results are wrapped in a `NetworkResult` sealed class. |
| Services | `services` | `NetworkMonitorService`, which publishes connectivity changes. |

---

## 3. Navigation

Navigation uses **Navigation Compose** with string routes and horizontal slide transitions (400 ms, `EaseOut`):

| Route | Screen | Purpose |
|---|---|---|
| `home` | `HomeView` | Animated title and entry points: *Play*, *Your games*, *Settings* |
| `game` | `GameView` | Game menu (category/difficulty), in-game screen, game-over screen, network-error screen |
| `stats` | `StatsView` | List of past games, and the game-details view with every question asked |
| `settings` | `SettingsView` | Theme and sound toggles, and a credits link |

`GameView` is a small state machine driven by LiveData:

```
isOffline == true                   → NetworkErrorScreen
!isPlaying && !isApiSetupComplete   → setupAPI() once, then GameViewMainPage
isPlaying                           → GameViewInGame (question / game over)
otherwise                           → GameViewMainPage
```

The system back button is intercepted with `BackHandler` in `MainActivity`:
- During a game it opens an exit-confirmation dialog.
- While offline it quits and saves the game immediately.
- On the game-over screen it returns to the game menu.
- In the stats screen it closes the details view before leaving the screen.

---

## 4. ViewModels and UI State

All ViewModels extend `AndroidViewModel`, because they need the `Application` context for Room, SoundPool/MediaPlayer and SharedPreferences. They are created with `viewModel<T>()` inside the `NavHost`, so they are **shared across destinations** and survive configuration changes such as rotation.

### GameViewModel
Owns the whole game session:
- **Selection:** `selectedCategory`, `selectedDifficulty` (default `Mixed`).
- **Session state:** `isPlaying`, `isGameOver`, `score`, `lives`, `isRecord`, `lastGame`.
- **Current question:** `questionForUser`, `shuffledAnswers`, `userAnswer`, `isAnswerSelected` (prevents double taps).
- **Timing:** `elapsedTime` (`mm:ss`), `isGameTimerInterrupted`.
- **Readiness:** `isApiSetupComplete`.

State is kept in private `MutableLiveData` and exposed as read-only `LiveData`.

### StatsViewModel
Loads all stored games, the questions of a selected game (via the `games_questions` link table) and deletes the history.

### SettingsViewModel
Reads and writes the dark-theme and sound flags through `PreferencesManager` (SharedPreferences). Every screen wraps its content in `WhoKnowsTheme(darkTheme = …)`, so a toggle applies instantly across the app.

---

## 5. Concurrency Model

All asynchronous work uses **Kotlin coroutines**:

- **`viewModelScope`:** every user action that touches I/O (`onStartClicked`, `onAnswerClicked`, `onQuitGameClicked`, stats loading) launches a coroutine that is cancelled automatically when the ViewModel is cleared.
- **`Dispatchers.IO`:** repository functions that call the network switch context with `withContext(Dispatchers.IO)`. Room DAOs are `suspend` functions, and Room runs them off the main thread.
- **Game timer:** a coroutine loop increments a counter every second with `delay(1000)` and pauses while `isGameTimerInterrupted` is `true`. It pauses while waiting for the API, while offline, and between questions.
- **API rate limiter:** after every request a `Job` (`apiTimerJob`) runs `delay(5200)`. The next request `join()`s that job first, which guarantees the OpenTDB limit of one request every 5 seconds without blocking any thread.
- **Answer feedback:** after an answer, `delay(500)` leaves time for the green or red highlight and the sound before the next question loads.
- **Retry on failure:** if a question cannot be fetched, `nextQuestionWithRetry()` suspends with `delay()` until connectivity is back, then retries. It stops if the player quits. It never blocks the main thread.
- **Atomic writes:** `GameQuestionDAO.insertGameWithQuestions` is a Room `@Transaction`, so a game and its question links are stored together.

Cancellation is respected: repositories rethrow `CancellationException` instead of converting it into an error result.

---

## 6. Networking

`QuestionRepository` builds a Retrofit client on `https://opentdb.com/` with a Gson converter. The `ApiService` interface declares four endpoints:

| Call | Endpoint | Used for |
|---|---|---|
| `getCategories()` | `api_category.php` | Populate `CategoryManager` (name → id map) |
| `getToken()` | `api_token.php?command=request` | Obtain a session token |
| `resetToken(token)` | `api_token.php?command=reset` | Reset an exhausted token |
| `getQuestions(amount, category, difficulty, token)` | `api.php` | Fetch the next question |

Details:
- **Custom deserializer:** OpenTDB returns HTML-encoded text (for example `&quot;`, `&#039;`). `QuestionDeserializer` decodes question, answers and category with `HtmlCompat.fromHtml` before building the `Question` entity.
- **Error model:** repository functions return `NetworkResult.Success(data)` or `NetworkResult.Error(exception)`. HTTP errors, I/O errors, non-zero OpenTDB response codes and SQLite errors all become `Error`, so the ViewModel can decide how to react instead of crashing.
- **Session token lifecycle:** requested once at setup and reset at the start of every game. When a query returns response code 4 (*token empty*), the token is reset and the query is retried once.

---

## 7. Connectivity and Offline Handling

`NetworkMonitorService` is a started `Service`:
- On start, it checks the active network synchronously (`ConnectivityManager.getNetworkCapabilities`) and sets `isOffline`.
- It registers a **default-network** callback (`registerDefaultNetworkCallback`), so only the network the system is actually using counts. `onAvailable` marks the app online. `onLost` re-checks the active network, because the system may already have switched, for example from Wi-Fi to mobile data. The result is posted to a companion-object `LiveData<Boolean>`.
- `MainActivity` starts monitoring in `onCreate` and `onResume` and stops it in `onStop` and `onDestroy`, so no callback stays registered while the app is in the background.

Behaviour when the connection drops:

| Situation | Behaviour |
|---|---|
| Offline in the game menu | Network-error screen with a *Go back* button |
| Connection lost mid-game | Network-error screen. The timer pauses and the player can wait or *Quit game*, which saves the game with its current score |
| Connection restored mid-game | The pending question request is retried and the timer resumes |
| Browsing history | Works fully offline, because it only reads Room |

Questions are persisted locally for the history, but new games still need the API (see *Known Limitations* in the README).

---

## 8. Persistence

- **Room database** `whoknows.db` (version 2) with three entities: `Question`, `Game` and `GameQuestion`. The schema is described in [TECHNICAL_DESIGN.md](TECHNICAL_DESIGN.md#4-database-schema).
- Access goes through a thread-safe singleton (`@Volatile` + `synchronized` double-checked locking).
- A `TypeConverter` stores `List<String>` (incorrect answers) as a JSON array and can still read the legacy comma-separated format.
- **SharedPreferences** (`app_prefs`) holds `isDarkTheme` and `isSoundEnabled`.

---

## 9. Lifecycle and Audio

- Answer sounds use a `SoundPool`, which gives low latency for short clips.
- The soundtrack uses a looping `MediaPlayer`, encoded as OGG Vorbis.
- `onPause` stops the soundtrack. `onResume` restarts it only if a game is in progress.
- Both players are released in `GameViewModel.onCleared()`.
- Sounds respect the *sound enabled* preference.

---

## 10. Testing and CI

- **Unit tests** (`app/src/test`, JUnit 4) cover the pure logic: `GameRules` (scoring, shuffling, API parameter mapping, timer formatting), the Room `Converters` (round trip, answers with commas, legacy format) and `CategoryManager`.
- **GitHub Actions:**
  - `android-ci.yml` runs the unit tests and builds the debug APK on every push and pull request, and uploads the APK and test reports.
  - `release.yml` builds and attaches the APK to a GitHub Release whenever a `v*` tag is pushed.

---

## 11. Dependencies

Versions are centralised in `gradle/libs.versions.toml`.

| Area | Library |
|---|---|
| Build | AGP 8.6, Kotlin 1.9, Gradle 8.7, kapt |
| UI | Compose BOM 2024.09, Material 3, Material Icons Extended, Navigation Compose 2.8 |
| Architecture | Lifecycle 2.8 (ViewModel, LiveData), Compose runtime-livedata |
| Networking | Retrofit 2.9, Gson converter, Gson 2.11 |
| Persistence | Room 2.6 (runtime, ktx, compiler) |
| Testing | JUnit 4 |

Target SDK 34, minimum SDK 28 (Android 9.0).
