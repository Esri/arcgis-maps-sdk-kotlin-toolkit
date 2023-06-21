# FeatureForm

## Description

The Forms toolkit component enables users to edit field values of features in a layer using forms that have been configured externally (using either in the the Web Map Viewer or the Fields Maps web app).

## Behavior

To see it in action, check out the [microapp](../../microapps/FeatureFormsApp).

## Usage

The `FeatureForm` composable is provided with its `FeatureFormState` and its default implementation `FeatureFormStateImpl` which is also available through the Factory function `FeatureFormState()`.
They can be used either as a simple state class or within a ViewModel.


#### Creating the state using the Factory function

```kotlin
val formState = FeatureFormState()
```

#### Using ViewModels

```kotlin
class MyViewModel : FeatureFormState by FeatureFormState() {
    ...
}
```

#### Creating the FeatureForm

The FeatureForm should be displayed in a Container and passed the `FeatureFormState`. 
It's visibility and the container are external and should be controlled by the calling Composable.

```kotlin
@Composable
fun MyComposable() {
    // use the inEditingMode to control the visibility of the FeatureForm
    val inEditingMode by formState.inEditingMode.collectAsState()
    // a container
    MyContainer(modifier = Modifier) {
        if (inEditingMode) {
            // show the FeatureForm only when inEditingMode is true
            FeatureForm(
                // pass in our FeatureFormState
                featureFormState = formState,
                // control the layout using the modifier property
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

#### Updating the `FeatureFormState`

Changes to the `FeatureFormState` will cause a recomposition.

```kotlin
@Composable
fun MyComposable() {
    ....
    // set the feature, this causes recomposition
    formState.setFeature(feature)
    // set formViewModel to editing state
    formState.setEditingActive(true)
    ....
}
```
