# Requirements Document
**Project:** ¿WhoKnows?: Android Trivia Game
**Context:** Mobile Programming course project, University of Rome Tor Vergata (A.Y. 2023/24)
**Version:** 1.0

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [User Stories](#2-user-stories)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Out of Scope / Future Work](#5-out-of-scope--future-work)

---

## 1. Product Overview

¿WhoKnows? is a native Android trivia game. Players answer questions drawn from the public Open Trivia Database, in a category and at a difficulty of their choice, until they lose all their lives. The app keeps a local history of every game so players can review their answers and track their progress.

The course asked for an Android application that:
- consumes a **public REST API**;
- **persists data locally**;
- performs **asynchronous work** without blocking the UI;
- provides a **modern, responsive UI**.

---

## 2. User Stories

*   As a player, I want to **choose a category and a difficulty** before starting, so that I can play on topics I like and at a level that suits me.
*   As a player, I want to **play a mixed game** (any category and/or any difficulty), so that every game feels different.
*   As a player, I want to **never see the same question twice in a session**, so that the game stays challenging.
*   As a player, I want to **lose a life on every wrong answer** and have the game end when none are left, so that every answer matters.
*   As a player, I want **harder questions to be worth more points**, so that taking risks is rewarded.
*   As a player, I want **immediate visual and audio feedback** when I answer, so that I know at once whether I was right.
*   As a player, I want to **see how long I have been playing**, so that I can measure my pace.
*   As a player, I want to be **told when I set a new record**, so that I feel motivated to improve.
*   As a player, I want to **browse my past games** and see every question with the answer I gave, so that I can learn from my mistakes.
*   As a player, I want to **delete my game history**, so that I can start fresh.
*   As a player, I want the app to **handle a lost connection gracefully**, so that I don't lose my game or see a crash.
*   As a player, I want to **switch between light and dark theme** and **mute sounds**, and have the app remember my choice.
*   As a player, I want the app to **work in both portrait and landscape**.

---

## 3. Functional Requirements

### 3.1 Game Setup
| ID | Requirement |
|---|---|
| FR-01 | On first access to the game screen, the app downloads the list of categories from OpenTDB. |
| FR-02 | The player can select one category or *Mixed* (any category). |
| FR-03 | The player can select *Easy*, *Medium*, *Hard* or *Mixed* difficulty. |
| FR-04 | The app obtains an OpenTDB session token and resets it at the start of every game. |

### 3.2 Gameplay
| ID | Requirement |
|---|---|
| FR-05 | A game starts with 3 lives and a score of 0. |
| FR-06 | Each question shows its text, its difficulty (stars), and its answers in random order. True/false questions show two options, multiple-choice questions four. |
| FR-07 | Only one answer per question can be selected. |
| FR-08 | A correct answer adds 1, 2 or 3 points (easy, medium, hard). A wrong answer removes one life. |
| FR-09 | The selected answer is highlighted green (correct) or red (wrong) and a matching sound plays. |
| FR-10 | An elapsed-time counter (`mm:ss`) is shown and pauses while no question is on screen. |
| FR-11 | The game ends when lives reach 0. The player can also quit at any time after confirming. |
| FR-12 | At the end of a game, the app shows the final score and a "New record!" message when the score beats all previous games. |
| FR-13 | When the session token runs out of questions, it is reset automatically and play continues. |

### 3.3 History
| ID | Requirement |
|---|---|
| FR-14 | Every finished or abandoned game is saved with score, category, difficulty, duration and date. |
| FR-15 | Every question asked is saved together with the answer given, and linked to its game. |
| FR-16 | The player can list past games and open a game to see its questions, each marked right or wrong. |
| FR-17 | The player can delete the whole history. |

### 3.4 Connectivity
| ID | Requirement |
|---|---|
| FR-18 | The app monitors connectivity in real time. |
| FR-19 | When offline, the game screen shows a dedicated message. During a game, the timer pauses and the player can quit and save. |
| FR-20 | When the connection returns, the pending request is retried and the game resumes. |
| FR-21 | The history is fully available offline. |

### 3.5 Settings
| ID | Requirement |
|---|---|
| FR-22 | The player can toggle light/dark theme and sound on/off. Both are persisted across launches. |
| FR-23 | A looping soundtrack plays during games when sound is enabled, and stops when the app goes to the background. |

---

## 4. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Responsiveness | Network and database work never runs on the main thread (coroutines, `Dispatchers.IO`, suspend DAOs). |
| NFR-02 | Robustness | Network, API and database failures are reported as results, not thrown, so the app does not crash when the connection drops. |
| NFR-03 | API etiquette | At most one request to OpenTDB every 5 seconds, as required by the API's rate limit. |
| NFR-04 | Compatibility | Android 9.0 (API 28) or later. Target API 34. |
| NFR-05 | Layout | Portrait and landscape are supported with dedicated layouts. |
| NFR-06 | Privacy | No account and no personal data. All history stays on the device. |
| NFR-07 | Maintainability | Layered MVVM architecture. Pure game rules are unit tested and verified in CI. |
| NFR-08 | Data integrity | A game and its question links are saved atomically in a single transaction. |

---

## 5. Out of Scope / Future Work

- **True offline play:** prefetching a buffer of questions so a game can start without a connection.
- **Online leaderboards or multiplayer:** would require a backend and user accounts.
- **Localisation:** the UI is English only, and OpenTDB questions are English only as well.
- **Accessibility audit:** content descriptions, contrast and font scaling were not formally verified.
