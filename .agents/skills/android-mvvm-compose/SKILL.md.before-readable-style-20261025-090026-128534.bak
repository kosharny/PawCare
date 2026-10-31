---
name: android-mvvm-compose
description: Build, change, review, or debug native Android applications in Kotlin using MVVM, Jetpack Compose, and compatible stable dependencies. Use for Android screens, ViewModels, persistence, navigation, dependency setup, and Gradle failures. Apply the user's current no-new-tests preference. Exclude iOS, Flutter, React Native, and backend-only work.
---

# Android MVVM + Compose

## Working agreement

- Reply in Russian unless asked otherwise. Use English identifiers and follow the project's UI language.
- Respect project instructions and existing conventions. Implement working vertical features rather than placeholder screens.
- Do not write or modify automated tests, add testing libraries, generate sample test classes, or introduce test/benchmark tasks into CI unless the user changes this preference. Preserve existing tests and checks; report relevant existing failures without suppressing them.
- Verify using compilation, Android Lint, and available manual runtime checks. Do not represent these as automated test coverage.

## Start

1. Read applicable `AGENTS.md` files and inspect the repository, version catalog, wrapper, Gradle plugins, manifest, SDK levels, and existing feature patterns.
2. Establish the actual Android Studio/JDK/SDK environment and build variants. Inspect relevant local files before browsing for setup guidance. Do not expose secrets from signing files or `local.properties`.
3. For dependency or build changes and new projects, read [dependencies.md](references/dependencies.md). Check official release channels and compatibility; do not guess versions or select prereleases from a documentation snippet.
4. For new screens, state, data flow, or architecture changes, read [architecture.md](references/architecture.md).
5. For a focused fix, preserve working versions and architecture unless changing them is necessary to resolve the problem.

## Defaults

- Use native Kotlin, a single Activity, Jetpack Compose, Material 3, AndroidX ViewModel, Coroutines, Flow, and StateFlow.
- Use MVVM with unidirectional data flow: UI actions → ViewModel → repository → data source; observable state → UI.
- Use feature packages inside a single `:app` module initially. Add domain use cases and Gradle modules only for concrete complexity or reuse.
- Use stable Navigation 3 for a new Compose-only application's navigation, after checking current integration and SDK requirements. Preserve working Navigation Compose in an existing app unless migration is requested or necessary.
- Use constructor injection. Prefer Hilt with KSP for a multi-screen app; keep manual DI in genuinely small apps and preserve an existing sound DI choice.
- Use Room for structured persistent records and DataStore for preferences when required. Add networking, image loading, and background scheduling only when a feature needs them.
- Use Gradle Kotlin DSL, the Gradle Wrapper, and `gradle/libs.versions.toml` for explicit pinned versions. Coordinate Compose libraries through the stable Compose BOM.
- Choose SDK and JDK settings from product requirements and current compatibility tables. Do not hardcode a universal minSdk or assume the Gradle runtime JDK must equal the bytecode target.

## Implementation discipline

- Keep repositories and data sources out of composables. Keep Activity, Context, Views, and navigation controllers out of ViewModels.
- Expose immutable `UiState` through read-only StateFlow and collect with `collectAsStateWithLifecycle()` at the route boundary. Keep screen content previewable through state and callbacks.
- Scope ViewModels to their screen or navigation entry. Avoid one global ViewModel for the whole app.
- Use structured concurrency and preserve cancellation. Keep blocking operations off the main thread and make repository APIs main-safe.
- Handle loading, empty, success, validation, and error states. Prevent duplicate submissions and explain recoverable errors in the UI.
- Persist real data. Keep preview samples out of production repositories. Preserve schema migrations and existing user records.
- Use localized string resources, semantic Material theme tokens, accessible controls, insets, and layouts that tolerate large fonts and different window sizes.
- Include all imports, manifest entries, generated-code setup, and dependencies needed for the implementation. Do not fabricate library APIs or leave nonfunctional buttons.

## Verification and handoff

1. Inspect the diff for scope, accidental files, missing imports, migrations, manifest changes, and unnecessary dependencies.
2. For production code/build changes, run the project's existing equivalent of `./gradlew :app:assembleDebug :app:lintDebug` with the appropriate module/variant. These tasks compile and analyze the app; do not add or invoke automated test tasks by default.
3. For metadata/documentation-only changes, use relevant structural checks instead of building the entire app.
4. If an SDK, JDK, network dependency, emulator, or device is unavailable, state precisely what was and was not verified. Do not disable lint or existing CI checks to obtain a green result.
5. When runtime access is available, exercise the changed user journey and relevant back navigation, permission, persistence, rotation, or process-restoration behavior. Otherwise provide a short manual checklist.
6. Summarize delivered behavior, chosen dependency versions and sources when changed, commands and outcomes, and any remaining concrete blocker.
