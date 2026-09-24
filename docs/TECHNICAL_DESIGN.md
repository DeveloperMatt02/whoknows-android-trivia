# ¿WhoKnows?: Technical Design Document

## 1. Product Overview
**¿WhoKnows?** is a single-player trivia game for Android. The player picks a category and a difficulty and answers multiple-choice or true/false questions until they run out of lives. Questions come from the public [Open Trivia Database](https://opentdb.com/). Every game is recorded locally, so players can look back at their past games, see which questions they got wrong, and try to beat their personal best.

The project was developed as the course project for *Mobile Programming* (University of Rome Tor Vergata, A.Y. 2023/24). It shows a modern Android stack: Kotlin, Jetpack Compose, MVVM, coroutines, Retrofit and Room.

## 2. Game Rules

| Rule | Value |
|---|---|
| Starting lives | 3 |
| Wrong answer | −1 life |
| Correct answer (easy) | +1 point |
| Correct answer (medium) | +2 points |
| Correct answer (hard) | +3 points |
| End of game | Lives reach 0, or the player quits |
| Record | The final score is strictly greater than every stored score |

With *Mixed* difficulty, each question keeps its own difficulty, and points follow the question. The rules live in `utils/GameRules.kt` and are unit tested.

## 3. Game Flow

### 3.1 Start-up
1. `MainActivity` starts `NetworkMonitorService`.
2. When the game screen is first shown online, `GameViewModel.setupAPI()`:
   - downloads the category list and builds `CategoryManager` (name → id);
   - requests an OpenTDB session token.
3. If setup fails (for example no connection), the error is logged and setup is retried when the next game starts.

### 3.2 Playing
```
Start ─► reset session token, score = 0, lives = 3, clear asked questions
      ─► fetch question (respecting the 5 s rate limit) ─► store it in Room
      ─► shuffle answers ─► start timer + soundtrack

Answer ─► save the given answer in Room ─► correct? ─┬─ yes ─► +points, ✓ sound, wait 500 ms ─► next question
                                                      └─ no ──► −1 life, ✗ sound, wait 500 ms
                                                                   ├─ lives > 0 ─► next question
                                                                   └─ lives = 0 ─► Game over
Game over / Quit ─► check record ─► save Game + GameQuestion links (transaction)
                 ─► stop soundtrack ─► show score (and "New record!")
```

### 3.3 History
`StatsView` loads all games. Selecting one loads the IDs of its questions from `games_questions`, then the full `Question` rows, including the answer the player gave. Correct answers are marked with ✓ and wrong ones with ✗. The whole history can be deleted from the top bar.

## 4. Database Schema

Room database `whoknows.db`, version 2.

### `questions`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | auto-generated |
| `type` | TEXT | `multiple` or `boolean` |
| `difficulty` | TEXT | `easy` / `medium` / `hard` |
| `category` | TEXT | category name, HTML-decoded |
| `question` | TEXT | HTML-decoded |
| `correctAnswer` | TEXT | |
| `incorrectAnswer` | TEXT | JSON array of strings (`TypeConverter`) |
| `categoryId` | TEXT | OpenTDB category id |
| `date` | TEXT | `yyyy-MM-dd HH:mm:ss`, when the question was fetched |
| `givenAnswer` | TEXT | the player's answer, empty if not answered |

### `games`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | auto-generated |
| `score` | INTEGER | |
| `difficulty` | TEXT | `Mixed` when no difficulty was chosen |
| `category` | TEXT | `Mixed` when no category was chosen |
| `duration` | TEXT | `mm:ss` |
| `date` | TEXT | `dd-MM-yyyy HH:mm:ss` |

### `games_questions` (link table)
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | auto-generated |
| `gameID` | INTEGER | → `games.id` |
| `questionID` | INTEGER | → `questions.id` |

A game and its links are written in a single `@Transaction`.

## 5. API Integration

**Base URL:** `https://opentdb.com/`. No authentication is required.

| Constraint | Handling |
|---|---|
| One request every 5 s per IP | A coroutine `Job` delays 5.2 s after each request, and the next request waits for it. The game timer is paused during the wait. |
| HTML entities in responses | `QuestionDeserializer` decodes them with `HtmlCompat.fromHtml`. |
| Repeated questions | A session token is sent with every request and reset at the start of each game. |
| Response code 4 (token empty) | Reset the token and retry once. |
| Other non-zero codes (1 no results, 2 invalid parameter, 3 token not found, 5 rate limit) | Returned as `NetworkResult.Error`. The ViewModel retries after the rate-limit delay. |
| Tokens expire after 6 h of inactivity | A new token is obtained at every app start. |

The app requests **one question at a time** (`amount=1`), so the selected category and difficulty apply immediately and nothing is downloaded that won't be used.

## 6. UI Design

- **Framework:** Jetpack Compose with Material 3. The custom colour scheme is yellow/amber (`#FFC107`), with light and dark variants and dynamic colour disabled for a consistent brand look.
- **Typography:** *Chakra Petch* for the UI and *Nabla* (a colour font) for the animated title. In the title, each letter bounces with a staggered infinite transition.
- **Responsive layouts:** `isLandscape()` switches between portrait and landscape variants of the home buttons, the game board (question, answers, lives, difficulty stars, score) and the stats rows.
- **Feedback:** the selected answer turns green (correct) or red (wrong). Sound effects play, hearts animate when a life is lost, and the score counter animates.
- **Brand assets:** the isometric "¿?" logo, the puzzle pattern used as the app background (`puzzle_bg_black` / `puzzle_bg_white`) and the promotional banner are collected in `docs/images/`; app screenshots are in `docs/screenshots/`.
- **Theme and sound:** toggled from the top bar or the settings screen, persisted in SharedPreferences and applied app-wide instantly.

## 7. Known Technical Debt

| Item | Impact | Possible improvement |
|---|---|---|
| Manual dependency wiring in ViewModels | Harder to unit test ViewModels | Hilt or a simple service locator |
| `LiveData` with many independent flags | State combinations are implicit | `StateFlow` with one immutable UI-state class per screen |
| One question per request, no prefetch | No offline play, 5 s wait between questions | Prefetch a buffer of questions per game |
| `fallbackToDestructiveMigration()` | Schema changes wipe the history | Explicit Room migrations and an exported schema |
| Deleting the history only clears `games` | Orphan rows remain in `questions` and `games_questions` | Foreign keys with `ON DELETE CASCADE` |
| Hard-coded UI strings | No localisation | Move them to `strings.xml` |
| Large composable files (`GameView.kt` ~1.8k lines) | Harder to navigate | Split into per-component files |
