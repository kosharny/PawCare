# Kotlin and Compose readability

Read this reference for Kotlin, Compose, or Gradle Kotlin DSL source edits. Source files must be comfortable to review, even when shorter code would use fewer tokens.

## Required layout

- Put Compose children, callbacks, lambda bodies, and coroutine bodies on their own indented lines. Expand one-statement UI handlers as well.
- Use one modifier per line for Compose Modifier chains and one continuation call per line for multi-step flow/collection/builder pipelines. Keep the original order.
- Wrap long signatures, data-class constructors, and calls vertically. Use named arguments when they explain non-obvious parameters and trailing commas where the configured toolchain supports them.
- Prefer descriptive names and named components to deeply nested inline expressions. Keep simple value assignments readable; do not expand ordinary property access or imports into artificial chains.
- Use one statement per line, braces around nontrivial control flow, and one blank line between distinct logical phases. Avoid semicolon-packed declarations and one-line composable trees.

Prefer this layout:

```kotlin
@Composable
fun SaveCareButton(
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {
            onSave()
        },
        enabled = !isSaving,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.save),
        )
    }
}
```

The example illustrates formatting, not a standalone file: use the real project's imports, resources, and types. For longer callbacks, use a clear handler or ViewModel action instead of hiding business logic inside UI code.

## Formatter workflow

1. Inspect `.editorconfig`, build plugins/version catalog, source sets, and existing `ktlint*` or `spotless*` tasks. Reuse one existing formatter; do not run competing ktfmt/ktlint/IDE styles back-to-back.
2. If setup is requested and no formatter exists, prefer ktlint through an appropriate supported Gradle integration, such as the project's Spotless or ktlint plugin. Verify current stable plugin/engine compatibility and pin versions. Do not invent Gradle tasks or paste an unverified plugin version.
3. Merge the [starter configuration](../assets/formatting/kotlin.editorconfig) into the project `.editorconfig`, preserving unrelated sections. Check that every rule/property exists in the pinned ktlint version. The chain-continuation rule is explicitly enabled for Android Studio style and the threshold is two chain operators; the naming exception preserves normal `@Composable` naming.
4. Set formatter targets/exclusions to first-party Kotlin/Kotlin DSL sources. Exclude generated outputs, vendored code, `.gradle`, and build directories. Honor the Android no-new-tests policy: do not autoformat existing test files as an incidental change.
5. Discover and run the actual apply task and check task. Typical integrations expose `./gradlew spotlessApply` / `./gradlew spotlessCheck` or `./gradlew ktlintFormat` / `./gradlew ktlintCheck`; use only the pair that exists. For a focused change, use the integration's documented file filtering or ratchet support. An unrestricted apply task may reformat the whole repository.
6. Review the diff for unintended edits and remaining compressed handlers/builders. Formatter output does not prove every requested readability preference is enforced. If a rule re-collapses required multiline code, resolve its supported configuration instead of oscillating between formatters or suppressing checks.
7. Continue the relevant build and Android Lint checks for production changes. Formatting checks are separate from Android Lint and do not require adding automated tests. State whether commands ran and what remains unavailable.

Do not create a whole-repository formatting CI or autoformat-on-every-build hook as an incidental change. Prefer an explicit format command and a non-mutating check in an existing suitable validation path when setup is authorized. Apply the same formatter configuration in the IDE to avoid formatting churn.

## Primary sources

- ktlint configuration: https://ktlint.github.io/ktlint/latest/rules/configuration-ktlint/
- Standard rules and chain wrapping: https://ktlint.github.io/ktlint/latest/rules/standard/
- Spotless Gradle integration: https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md

Refresh rule availability against the selected release. A starter `.editorconfig` alone does not install or execute ktlint.
