This document is a high level summary of testing effort for the Kotlin toolkit.

# Testing Goals
* Test toolkit component implementations and patterns.
* UI Testing with Jetpack Compose layouts.
* Fulfill feature test designs for Native Maps SDK features which are implemented in the toolkit repo.

## UI Testing

Using [semantics](https://developer.android.com/jetpack/compose/semantics) with Compose allows us to test the layout of the composition tree. This would mean adding semantic information during the development of the composable.

[Compose testing cheat sheet](https://developer.android.com/static/images/jetpack/compose/compose-testing-cheatsheet.pdf)
