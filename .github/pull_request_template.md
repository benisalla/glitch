## What and why
<!-- One or two sentences. Link the issue if there is one. -->

## Checklist
- [ ] Stays within the product scope in `AGENTS.md` (no new permissions, network code or tracking)
- [ ] New behavior in `menu/` or `apps/` has tests; shuffle tests use a seeded `Random`
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passes locally
- [ ] Instrumented tests run on an emulator or device, if UI or app discovery changed
