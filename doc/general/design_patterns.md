# Design patterns and best practices

This document outlines guidelines for best practices and design patterns in the writing and development of Jetpack Compose APIs as part of the ArcGIS Maps SDK for Kotlin toolkit.

# Naming conventions

[Compose baseline](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#compose-baseline) provides a set of coding conventions that define the naming rules for compose based APIs. 

# Architectural Patterns

The architecture, pattern and design recommendations are intended to improve the usability, robustness and scalability of the toolkit. The suggested principle is a layered architecture embodying separation of concerns. This should include at least two layers:
- UI Layer
  - The UI layer is only responsible for interaction and presenting the data onto the screen. UI Elements such as composables and state holders like ViewModels make up this layer.
- Data Layer
  - The data layer fetches, stores and exposes data sources to the UI Layer.

## ViewModel

ViewModels are the primary way to expose UI states to composables. They allow the application to adhere to the architectural principle of basing the UI on the model by transforming application data into UI states. 
- [Lifecycle of a ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel#lifecycle)
- [Benefits of a ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel#viewmodel-benefits)

### Best practices

- In Jetpack Compose, ViewModel instances should be created and accessed close to their root composable. This is because any composable accessing a ViewModel under its [ViewModelStoreOwner](https://developer.android.com/reference/kotlin/androidx/lifecycle/ViewModelStoreOwner) will receive the same instance.
- ViewModels must not be passed down to lower level composables and instead only pass the data and logic required.
- To avoid memory leaks, ViewModels should avoid having any references to lifecycle components like `Context` or `Resources`.
- Method names and state fields on the ViewModel must be generic in nature since ViewModels should not know about specific UI implementation details.
    - Example - Signature of `fun showFeatureFormEditor` is generic and the UI component implementation details will display appropriate UI for a phone or a tablet.
    ```kotlin
    class FeatureFormViewModel() : ViewModel() {
        ..
        fun showFeatureFormEditor() { .. }
        ..
    }
    ```

### Creating ViewModels

A ViewModel can be created and accessed inside a Composable using [viewModel()](https://developer.android.com/reference/kotlin/androidx/lifecycle/viewmodel/compose/package-summary#viewmodel)

```kotlin
@Composable
fun MainScreen() {
    ...
    val mapViewModel = viewModel<MapViewModel>()
    ...
}
```

For a ViewModel with dependencies, a `Factory` can be used.


```kotlin
class MapViewModelFactory(private val mapData: MapData) 
    : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(mapData) as T
    }
}
```

```kotlin
@Composable
fun MainScreen() {
    val mapData = MapData(
        map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    )
    val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(mapData))
    ...
}
```

### Coroutines and ViewModels

[Use Kotlin coroutines with lifecycle-aware components](https://developer.android.com/topic/libraries/architecture/coroutines)

## State Hoisting

All data must flow from a single source of truth and only its owner can mutate the data. Often ViewModels host this source of data or in case of UI state holders, they should be hosted at the root composable level.

```kotlin
class MapViewModel (mapData: MapData) : ViewModel() {
    private val _mapData: MutableStateFlow<MapData> = MutableStateFlow(mapData)
    // exposed as immutable type and hoisted at the ViewModel
    val mapData = _mapData.asStateFlow()
}
```

## Unidirectional Data Flow

With a unidirectional data flow design, all data and states must flow down the hierarchy from the data sources to the ViewModels to the composable UI. All events and UI actions must flow up the same hierarchy.

#### Events

```kotlin
@Composable 
fun LoadingScreen(modifier: Modifier = Modifier, onTimeout: () -> Unit) {
    val currentTimeout by rememberUpdatedState(onTimeout)
    ...
    LaunchedEffect(Unit) { 
        delay(waitTime)
        currentTimeout() 
    }
}
```

#### UI actions

```kotlin
@Composable
fun MyScreen(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
    ...
    Button(onClick = {
        onBackPressed()
        }) {
        Icon( ... )
    }
}
```


## Composable Map

The `ComposableMap` component offers a Composable `MapView` by wrapping it within an `AndroidView`.

### Challenges
- AndroidView and MapView challenges (To-do)

## More Recommendations

[Recommendations for Android architecture](https://developer.android.com/topic/architecture/recommendations)

# States in Compose

## Flows

# Side Effects

Side effects are any changes or operations that need to occur outside the scope of a composable. The Effect APIs offer a controlled environment that is tied to the lifecycle of the composable. These effects are composable functions themselves but do not emit any UI.

## LaunchedEffect

`LaunchedEffect` allows us to safely run coroutines within the `CoroutineScope` of a composable. The coroutine is cancelled when the composition ends and the `LaunchedEffect` leaves the composition. Similarly, if the key in `LaunchedEffect(key)` changes due to recomposition, the running coroutine is cancelled and a new one is launched.

Any suspend functions or coroutines that need to run within a composable's scope must be done so within a `LaunchedEffect`.

Using `Unit` or `true` for the keys will only run the `LaunchedEffect` once. These should be used with careful consideration. MapView gesture events are `SharedFlow`s which will be continously collected until cancelled.
We expect them to be cancelled only when the containing Composable leaves the composition and collection should not be tied to state, so they are a good example of a use case for `LaunchedEffect(Unit)`.

```kotlin
LaunchedEffect(Unit) {
    launch {
        mapView.onSingleTapConfirmed.collect {
            // do something
        }
    }
}
```


Providing a key will ensure the `LaunchedEffect` runs on every recomposition if the key changes.

```kotlin
LaunchedEffect(mapState) {
    launch {
        // this will run on every recomposition when mapState changes
    }
}
```


## DisposableEffect

Similarly, any code that needs a clean up if the keys change or the composition ends should use a `DisposableEffect`. Event subscriptions, lifecycle callbacks are good examples for this.

```kotlin
DisposableEffect(lifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(mapView)
    // called when the composition ends or lifecycleOwner changes
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(mapView)
    }
}
```
