# Android-стек PawCare

Проверено 25 октября 2026 года по официальным источникам и фактическому окружению. Это существующий шаблон Android Studio; namespace/applicationId `com.example.pawcare` сохранены. Модуль один — `:app`.

| Компонент | Выбор | Источник / решение |
|---|---|---|
| Android Studio | Rabbit 1, 2026.2.1, build 262.9437.185.2621.16467767 | Установленная IDE, подтверждена `android studio check` |
| AGP | 9.4.1 | Существующая стабильная версия. [Совместимость AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes): Gradle 9.6, JDK 17+, максимум API 37 |
| Gradle Wrapper | 9.6.0 | Существующий wrapper JAR и SHA256 сохранены; это минимальная поддерживаемая AGP 9.4 версия. Более новый Gradle без необходимости не добавляется |
| JDK для Gradle | 21.0.2, установленный Oracle JDK | Удалены ссылки шаблона на автоматическую загрузку JDK 25. `gradle-daemon-jvm.properties` выбирает установленный JDK 21; bytecode target Java/Kotlin 17 |
| Kotlin / Compose compiler / serialization compiler | 2.4.21 | [Kotlin releases](https://kotlinlang.org/docs/releases.html), стабильный релиз от 08.10.2026. KGP явно добавлен на buildscript classpath; built-in Kotlin AGP получает согласованную более новую версию. `org.jetbrains.kotlin.android` не применяется. [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) |
| compileSdk / targetSdk | 37 | Сохранена установленная стабильная платформа `android-37.0`; AGP 9.4 её поддерживает |
| minSdk | 26 | Продуктовая граница из задания, нативный java.time. Шаблон содержал 24, но реализованных пользовательских данных/функций ещё не было. Android 7 не поддерживается |
| Compose BOM | 2026.09.00 | [Официальная стабильная BOM](https://developer.android.com/develop/ui/compose/bom); используется BOM без alpha/beta. Material 3 и Compose версии из BOM |
| Core / Activity | 1.19.1 / 1.13.0 | Существующие версии сохранены; [Core](https://developer.android.com/jetpack/androidx/releases/core), [Activity](https://developer.android.com/jetpack/androidx/releases/activity) |
| Lifecycle | 2.11.0 | [Stable release](https://developer.android.com/jetpack/androidx/releases/lifecycle), включая runtime-compose и viewmodel-navigation3 |
| Navigation 3 | 1.2.0 | [Stable release от 23.09.2026](https://developer.android.com/jetpack/androidx/releases/navigation3). В шаблоне не было навигации; выбран Navigation 3 согласно AGENTS.md. Ключи Serializable, rememberNavBackStack, saveable-state и ViewModel-store decorators |
| Room | 2.8.5 | [Room](https://developer.android.com/jetpack/androidx/releases/room), KSP, экспорт схемы version 1; destructive migration отсутствует |
| KSP | 2.3.12 | [Google KSP releases](https://github.com/google/ksp/releases), стабильный KSP2; работает с выбранным KGP, подтверждается компиляцией процессоров |
| Hilt / AndroidX Hilt | 2.60.1 / 1.4.0 | [Dagger](https://github.com/google/dagger/releases), [AndroidX Hilt](https://developer.android.com/jetpack/androidx/releases/hilt), поддержка AGP 9, KSP, ViewModel и WorkManager |
| Preferences DataStore | 1.3.0 | [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) |
| Coroutines | 1.11.0 | [Официальные releases](https://github.com/Kotlin/kotlinx.coroutines/releases), stable |
| Serialization core | 1.12.0 | [Официальные releases](https://github.com/Kotlin/kotlinx.serialization/releases), 19.10.2026, основана на Kotlin 2.4.10; используется для ключей навигации |
| WorkManager | 2.12.0 | [WorkManager](https://developer.android.com/jetpack/androidx/releases/work); периодическая сверка, доставка через Worker, без требования сети |
| Coil Compose | 3.6.3 | [Coil](https://coil-kt.github.io/coil/getting_started/); только локальные фото, сетевой модуль не добавлен |
| AppCompat | 1.8.0 | [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat); AppCompatActivity только для совместимого переключения языка, UI полностью Compose |
| ExifInterface | 1.4.2 | [ExifInterface](https://developer.android.com/jetpack/androidx/releases/exifinterface); ориентация и зеркальность фото |

Новые тестовые библиотеки не добавлены. Существующие шаблонные тесты и их зависимости сохранены. Автоматические тестовые задачи не запускаются по текущему решению владельца.

В приложении нет INTERNET, Firebase, аналитики, сервера, exact-alarm permissions и foreground service. Зависимости скачиваются Gradle только для сборки.
