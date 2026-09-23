# Contributing to ¿WhoKnows?

Thanks for your interest! This started as a university project, but issues and pull requests are welcome.

## Reporting bugs and suggesting features

Open an [issue](https://github.com/DeveloperMatt02/whoknows-android-trivia/issues) and include:
- what you expected and what happened instead;
- steps to reproduce;
- device or emulator model and Android version;
- a screenshot or a Logcat excerpt, if relevant.

## Development setup

1. Fork the repository and clone your fork.
2. Open the project in Android Studio (Koala or newer) with JDK 17.
3. Create a branch from `main`: `git checkout -b feature/short-description`.

## Before opening a pull request

- Run the unit tests: `./gradlew testDebugUnitTest`.
- Make sure the app builds: `./gradlew assembleDebug`.
- Follow the existing structure (MVVM: Compose screen → ViewModel → Repository → DAO/API).
- Keep pure logic (rules, parsing, formatting) out of ViewModels when possible, and add unit tests for it.
- Use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages, for example `fix: …`, `feat: …`, `docs: …`.

CI runs automatically on every pull request.

## Good first contributions

See *Known Limitations & Roadmap* in the [README](README.md) and *Known Technical Debt* in [TECHNICAL_DESIGN.md](docs/TECHNICAL_DESIGN.md). Question prefetching for offline play, moving strings to `strings.xml`, and cascading deletes for the history are all self-contained tasks.

## License

By contributing, you agree that your contributions will be licensed under the [GNU GPL v3.0](LICENSE).
