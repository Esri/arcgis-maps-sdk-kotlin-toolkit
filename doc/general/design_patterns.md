## Design patterns and best practices

This document outlines guidelines for best practices and design patterns in the writing and development of Jetpack Compose APIs as part of the Kotlin maps SDK toolkit.

## Naming conventions

[Compose baseline](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#compose-baseline) provides a set of coding conventions that define the naming rules for compose based APIs.

## Architecture
- microapps
  - ViewModel?
## States in Compose

### Flows

### remember state

## Unidirectional Data Flow

## Side Effects

Side effects are any changes or operations that need to occur outside the scope of a composable. The Effect APIs offer a controlled environment that is tied to the lifecycle of the composable. These effects are composable functions themselves but do not emit any UI.

### LaunchedEffect

`LaunchedEffect` allows us to safely run coroutines within the `CoroutineScope` of a composable. The coroutine is cancelled when the composition ends and the `LaunchedEffect` leaves the composition. Similarly, if the key in `LaunchedEffect(key)` changes due to recomposition, the running coroutine is cancelled and a new one is launched.

Any suspend functions or coroutines that need to run within a composable's scope must be done so within a `LaunchedEffect`.

Using `Unit` or `true` for the keys will only run the `LaunchedEffect` once.

    LaunchedEffect(Unit) {
        launch {
            mapView.onSingleTapConfirmed.collect {
                // do something
            }
        }
    }

Providing a key will ensure the `LaunchedEffect` runs on every recomposition.

    LaunchedEffect(mapState) {
        launch {
          // this will run on every recomposition when mapState changes
        }
    }

### DisposableEffect

Similarly, any code that needs a clean up if the keys change or the composition ends should use a `DisposableEffect`. Event subscriptions, lifecycle callbacks are good examples for this.

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)

        // called when the composable ends or lifecycleOwner changes
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }

