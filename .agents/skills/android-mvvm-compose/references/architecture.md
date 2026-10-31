# Architecture and implementation decisions

## Project shape

Start a small app in one application module. Organize Kotlin packages by responsibility:

| Package | Responsibility |
| --- | --- |
| `app` | Application, Activity, root composition, navigation host |
| `core/model` | Shared plain Kotlin models, only when actually shared |
| `core/ui` | Theme and reused UI components |
| `data/local` | Room database, entities, DAOs, preference storage |
| `data/remote` | API clients and DTOs, only when needed |
| `data/repository` | Repositories, data coordination, source-of-truth decisions |
| `di` | Hilt modules or a small explicit dependency container |
| `feature/<name>` | Route, Screen, ViewModel, UiState, feature UI components |
| `domain` | Optional reusable or complex pure business logic |

Treat these as packages, not a requirement to create every folder or a Gradle module for each. Follow established layout in an existing repository. Do not generate empty layers or one-line use cases for every repository call. Add repository interfaces when they establish a useful boundary, rather than mechanically doubling every class.

## Screen contract

- Separate `FeatureRoute` (ViewModel lookup, lifecycle-aware collection, navigation wiring) from `FeatureScreen` (state and callback rendering).
- Prefer an immutable `data class FeatureUiState` or an appropriate sealed hierarchy; expose `val uiState: StateFlow<FeatureUiState>`.
- Keep mutable StateFlow private. Derive state from repository flows using `combine`/`map` and `stateIn` when appropriate. Use `SharingStarted.WhileSubscribed(5_000)` for UI subscriptions when its semantics fit; do not apply it blindly to work that must continue without collectors.
- Use explicit ViewModel action functions. Introduce an action hierarchy only when it reduces complexity.
- Keep transient widget state such as an expanded menu local. Use `rememberSaveable` for suitable saveable local state and SavedStateHandle for small restoration inputs. Store durable user records in the data layer.
- Express important operation results as state with explicit UI acknowledgement. Do not use a lossy SharedFlow/Channel event as the only record of a completed save, purchase, or other important action.
- Keep navigation at the UI boundary. Navigate after an observed and acknowledged result when appropriate; avoid repeatedly navigating during recomposition.
- Scope ViewModels to the correct Navigation 3 entry and configure the current saveable-state and ViewModel-store integration. Verify back-stack restoration and predictive back using the supported APIs.

## Persistence and concurrency

- Put business data behind repositories even when there is one data source. Let a local database drive UI in an offline-first app.
- Use Room suspend writes and observable queries. Use transactions for related writes. Export schema history and implement migrations; do not add destructive migration fallback to hide schema errors.
- Use DataStore for preferences and small settings, not as a list database. Reuse one appropriate DataStore instance per file.
- Map database entities or network DTOs at a meaningful boundary. Avoid unnecessary identical model copies.
- Launch UI work in `viewModelScope`. Keep suspend operations main-safe in the layer that owns the blocking work; Room and other main-safe APIs do not require redundant IO wrappers.
- Rethrow CancellationException before general error handling. Avoid generic `runCatching` around suspending work unless cancellation is explicitly preserved.
- Do not use `GlobalScope`, `runBlocking` on UI paths, or an infinite coroutine as persistent scheduling.
- Make recurring or rescheduled background work idempotent. Distinguish delay-tolerant WorkManager jobs from precisely timed AlarmManager requirements; check current platform permissions and restrictions before choosing.
- Store calendar dates as date semantics and timestamps as instant semantics. Define time-zone and daylight-saving behavior for reminders; do not replace months with fixed 30-day intervals.

## UI quality

- Use a consistent Material 3 theme and resources for visible text. Respect the product's specified colors and language.
- Apply edge-to-edge insets deliberately; avoid applying the same padding twice. Account for the keyboard, display cutouts, and system navigation.
- Use lazy lists with stable item keys and meaningful content types where useful. Avoid expensive computation directly in composable bodies.
- Provide content descriptions for meaningful icons, adequate touch targets, readable contrast, large-font support, and meaningful semantics.
- Keep optional screens and UI experiments separate from working user flows. Add previews for reusable components and key screen states without requiring Hilt, a network, or a live database.
- Request permissions at the feature's point of use and handle denial. Use system pickers when appropriate instead of unnecessary broad storage access.

## Primary sources

- Architecture: https://developer.android.com/topic/architecture/recommendations
- State and events: https://developer.android.com/topic/architecture/ui-layer/events
- Navigation 3: https://developer.android.com/guide/navigation/navigation-3
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Room: https://developer.android.com/training/data-storage/room
- Background work: https://developer.android.com/develop/background-work

Open the relevant current documentation before relying on an unfamiliar or recently changed API.
