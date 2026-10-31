# Android project instructions

## Working agreement

- Communicate in Russian unless requested otherwise; use English code identifiers and the specified UI language.
- Use the `android-mvvm-compose` skill in `.agents/skills/android-mvvm-compose/` for native Android implementation, Gradle configuration, and code review. Read its references when the task needs them.
- Inspect the existing repository and preserve its working conventions. Complete usable features with correct imports, dependencies, manifest declarations, and persistence.
- Do not write or modify automated tests, add testing dependencies, or introduce test/benchmark CI jobs until the user explicitly changes this preference. Preserve existing tests and required checks; report failures without disabling them.
- Verify production changes through compilation, Android Lint, and available manual runtime checks. State what could not be verified.

## Architecture

- Use Kotlin, Jetpack Compose, Material 3, and MVVM with unidirectional state flow.
- Keep a screen-scoped ViewModel per meaningful screen/flow. Avoid one global ViewModel for the application.
- Expose immutable UiState with read-only StateFlow; collect it with `collectAsStateWithLifecycle()` at the route boundary.
- Separate Route (state collection and navigation wiring) from Screen (state and callbacks). Keep screens previewable.
- Route user actions through ViewModels and repositories. Keep databases, API clients, and business logic out of composables.
- Keep Activity, Context, Views, and navigation controllers out of ViewModels. Use structured concurrency and preserve cancellation.
- Organize by feature in one `:app` module initially. Add domain use cases, interfaces, and modules only when they solve concrete complexity or reuse.
- Use constructor injection; prefer Hilt with KSP for multi-screen apps and manual DI for tiny apps. Preserve an existing supported DI setup.
- Prefer stable Navigation 3 for a new Compose-only app; preserve an existing supported navigation solution unless migration is needed.
- Use Room for structured records and DataStore for preferences when required. Preserve existing data and implement schema migrations.

## Dependencies and build

- Use Gradle Kotlin DSL, Gradle Wrapper, and `gradle/libs.versions.toml` with pinned versions.
- For a new project or dependency upgrade, verify the newest compatible stable set from official release pages. Never guess version numbers or add alpha/beta/RC/SNAPSHOT dependencies by default.
- Check Android Studio, AGP, Gradle, JDK, effective Kotlin compiler, Compose compiler, KSP/processors, and SDK compatibility as a set.
- With AGP 9+ built-in Kotlin, do not also apply `org.jetbrains.kotlin.android`. Align Compose and serialization compiler plugins with the effective Kotlin compiler.
- Use stable Compose BOM for covered Compose artifacts. Version Lifecycle, Activity, Navigation, Room, Hilt, and other uncovered libraries separately.
- Record the checked date, chosen versions, official URLs, and compatibility decisions in `docs/android-stack.md` when setting up or changing the stack. For unrelated feature work, keep working versions unchanged.
- Add libraries only for implemented requirements. Do not add networking, Firebase, analytics, authentication, or AI by default.
- Select SDK levels and JDK settings from product requirements and current compatibility tables, not an arbitrary hardcoded template.

## UI and behavior

- Handle loading, empty, populated, error, validation, and permission-denied states where relevant.
- Persist user-created data. Keep preview samples out of production repositories.
- Use string resources, theme tokens, proper system/IME insets, accessible controls, and layouts supporting large text and variable window sizes.
- Use SavedStateHandle and rememberSaveable for appropriate restoration state; store durable records in the data layer.
- Keep important operation results in acknowledged state instead of relying solely on lossy one-time events.
- Implement background work with platform-supported scheduling and correct time/date semantics when required.

## Completion checks

- Inspect the diff for accidental changes, incomplete code, migrations, imports, and unnecessary dependencies.
- For code/build changes run the relevant equivalent of `./gradlew :app:assembleDebug :app:lintDebug`; adapt module/variant names to the project.
- Do not invoke automated test tasks by default or weaken lint/CI rules to hide failures. Use relevant structural checks for documentation-only changes.
- If runtime access exists, exercise the changed user journey, persistence, and relevant lifecycle/permission behavior. Otherwise provide a short manual checklist.
- Report delivered behavior, actual verification results, and concrete remaining blockers. Do not claim a build or runtime check passed unless it ran successfully.
