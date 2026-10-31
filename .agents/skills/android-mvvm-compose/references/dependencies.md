# Dependency selection and build compatibility

## Selection workflow

1. Inspect existing versions and the effective Kotlin setup, wrapper, Java runtime, bytecode target, SDKs, processors, and IDE compatibility.
2. For a new project or an intentional upgrade, consult official release pages. Choose the newest stable compatible set, not independently the highest number for every component. For a focused feature or bug fix, check only new or changed dependencies and leave unrelated versions alone.
3. Check the release-channel table and publication date. Official sample snippets sometimes use alpha versions even when stable versions exist. Do not copy a prerelease, a future-dated release, a dynamic `+`, `latest.release`, or a snapshot.
4. Record the verified selection in `docs/android-stack.md` when establishing or updating the stack. Include date, artifact/plugin, selected version, official source URL, relevant compatibility constraints, and any reason for using an older stable version. For an offline session, label versions as existing/unverified instead of claiming they are current.
5. Pin versions in the version catalog and wrapper; declare repositories centrally using `google()`, `mavenCentral()`, and required plugin repositories. Do not add JitPack or custom mirrors without a specific dependency need.
6. Resolve dependencies and compile/lint the chosen variant. Inspect `dependencies`/`dependencyInsight` only when needed to explain a conflict. Do not force or exclude dependencies broadly just to silence incompatibility.

## Compatibility rules

- Check AGP's table for the required Gradle runtime and JDK, supported SDK, and Android Studio support. Check plugin compatibility with AGP's DSL and variant APIs.
- AGP 9+ enables built-in Kotlin by default. Do not also apply `org.jetbrains.kotlin.android` to an Android module using built-in Kotlin. Determine the effective Kotlin compiler version from current AGP documentation/build resolution before setting Kotlin compiler plugins. Do not mix the old `kotlinOptions` pattern with the new DSL blindly.
- If an older project's AGP requires the standalone Kotlin Android plugin, preserve that supported configuration until an intentional migration. Do not disable built-in Kotlin or the new DSL as a default workaround.
- For Kotlin 2+, apply `org.jetbrains.kotlin.plugin.compose` in Compose modules, aligned to the effective Kotlin compiler. Enable `buildFeatures.compose = true`. Do not revive the old `kotlinCompilerExtensionVersion` setup.
- The stable Compose BOM aligns only the Compose artifacts listed in its mapping. It does not select Kotlin, the Compose compiler plugin, AGP, Activity, Lifecycle, Navigation, Hilt, Room, or Coroutines. Version those separately.
- Use the Kotlin serialization compiler plugin only if serialization code generation is needed; align it with the effective Kotlin compiler. Select the serialization runtime version separately.
- Prefer KSP for Room and Hilt when supported by the selected versions. Verify KSP/Kotlin/processor compatibility from current release notes; do not assume KSP always shares Kotlin's version format. Keep each processor and its runtime aligned according to upstream guidance. Do not configure kapt and KSP for the same processor.
- Choose stable compileSdk/targetSdk according to the current stable SDK, AGP support, release requirements, and behavior changes. Choose minSdk from product needs and the maximum minimum required by selected dependencies. Explain any product-impacting increase.
- Use the Gradle Wrapper. Do not add an arbitrary `buildToolsVersion` or manually pin Kotlin stdlib to mask incompatibility. Align JVM compilation settings intentionally.

## Stack by need

| Need | Default choice | Condition |
| --- | --- | --- |
| Language/UI | Kotlin, Compose, Material 3 | Native Android |
| State/lifecycle | ViewModel, Coroutines, Flow/StateFlow, lifecycle-runtime-compose | Screen state |
| Navigation | Stable AndroidX Navigation 3 | New Compose-only app; verify entry state/ViewModel integration |
| DI | Hilt with KSP | Multi-screen app; manual DI is sufficient for tiny apps |
| Structured records | Room with KSP | Actual persisted domain records |
| Preferences | DataStore | Small durable settings |
| HTTP API | Retrofit + OkHttp + Kotlin serialization | Only when an API is needed; verify official Square documentation and converter artifact |
| Image loading | Coil Compose | Only for async/local URI/remote image loading needs |
| Deferrable durable jobs | WorkManager | Only when background requirements justify it |
| Exact-time reminders | Platform AlarmManager | Only after assessing current restrictions and permission eligibility |

Do not add every row to every app. Keep offline apps free of unnecessary networking and do not add analytics, Firebase, authentication, or AI SDKs without a product requirement.

## Current official lookup points

- AGP: https://developer.android.com/build/releases
- Kotlin: https://kotlinlang.org/docs/releases.html
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Compose compiler: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Built-in Kotlin: https://developer.android.com/build/migrate-to-built-in-kotlin
- AndroidX releases: https://developer.android.com/jetpack/androidx/versions
- Navigation 3: https://developer.android.com/jetpack/androidx/releases/navigation3
- Lifecycle: https://developer.android.com/jetpack/androidx/releases/lifecycle
- Room: https://developer.android.com/jetpack/androidx/releases/room
- DataStore: https://developer.android.com/jetpack/androidx/releases/datastore
- Hilt/Dagger: https://github.com/google/dagger/releases
- KSP: https://github.com/google/ksp/releases
- Coroutines: https://github.com/Kotlin/kotlinx.coroutines/releases
- Retrofit: https://square.github.io/retrofit/
- OkHttp: https://square.github.io/okhttp/
- Coil: https://coil-kt.github.io/coil/

Treat these as live lookup sources, not a fixed version snapshot. If a link moves, search the same official publisher. Do not invent an exact version when the source is unavailable.
