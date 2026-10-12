# Contributing

1. Read [AGENTS.md](AGENTS.md) for scope, architecture, test and privacy rules. Changes outside the product scope will not be merged.
2. Create a feature branch from `main`.
3. Before opening a pull request, run:
   ```
   ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```
   Run `./gradlew :app:connectedDebugAndroidTest` too if you have an emulator or device available.
4. Open a pull request to `main`. CI must pass before merging. Do not force-push to `main`.
5. Do not commit local settings (`.claude/settings.local.json`, `local.properties`), secrets, keystores, or build output.

Claude Code (v2.1.277 or later) reads `AGENTS.md` directly, so the repo has no `CLAUDE.md`. Adding a `CLAUDE.md` or `CLAUDE.local.md` makes Claude Code read those instead of `AGENTS.md`; if you need one, start it with `@AGENTS.md`.
