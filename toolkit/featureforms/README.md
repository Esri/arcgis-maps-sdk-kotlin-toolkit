

# FeatureForm

## Description

The FeatureForm toolkit component enables users to edit field values of features in a layer using the `FeatureForm` API that has been configured externally (using either in the Web Map Viewer or the Fields Maps web app).

## Behavior

To see it in action, check out the [microapp](../../microapps/FeatureFormsApp).

## Features

The `FeatureForm` is a Composable that can render a `FeatureForm` object with a `FeatureFormDefinition`  using Jetpack Compose.
- It can be integrated into any custom layout or container. The [microapp](../../microapps/FeatureFormsApp) integrates it into a `BottomSheet`.
- All expressions are initially evaluated with a progress indicator before the FeatureForm is available for editing.
- Provides automatic saving and restoring of form data after device configuration changes.
- Shows validation errors for any fields with errors.
- Visibility behavior of validation errors can be customized.
- Supports all `FormInput` types except Attachments, Relationships and Barcodes.
- Provides a DateTime picker and a picker for coded-value field types.
- Follows material 3 design system.


## Usage

A `FeatureForm` composable can be created using a `FeatureForm` object as follows.

#### Creating a FeatureForm object

```kotlin
// get the feature layer
val layer = feature.featureTable.layer as FeatureLayer  
// grab the FeatureFormDefinition from the layer
val featureFormDefinition = layer.featureFormDefinition
// create the FeatureForm from the Feature and the FeatureFormDefinition
val featureForm = FeatureForm(feature, featureFormDefinition)
```

#### Creating a FeatureForm in Compose

A `FeatureForm` can be created within a composition by simply calling the `FeatureForm` composable with the `FeaureForm` object. The FeatureForm should be displayed in a container. It's visibility and the container are external and should be controlled by the calling Composable.

```kotlin  
@Composable  
fun MyComposable(featureForm : FeatureForm) {  
    // a container  
    MyContainer(modifier = Modifier) {
	// create a FeatureForm Composable
        FeatureForm(  
	    // pass in the FeatureForm object  
	    featureForm = featureForm,  
	    // control the layout using the modifier property  
	    modifier = Modifier.fillMaxSize()  
	)  
    }  
} 
```  

#### Updating the `FeatureForm`

To display a new `FeatureForm`  object, simply trigger a recomposition with the new `FeatureForm` object.

```kotlin  
@Composable  
fun MyComposable(viewModel : MyViewModel) {  
    // use a state object that will recompose this composable when the featureForm changes
    // in this example, the FeatureForm object is hoisted in the ViewModel
    val featureForm : State by viewModel.featureForm  
    // a container  
    MyContainer(modifier = Modifier) {
        FeatureForm(    
	    featureForm = featureForm,  
	    modifier = Modifier.fillMaxSize()  
	)  
    }  
}
```  

#### Changing the Validation Error Visibility policy

By default validation errors for any fields are only visible after the fields gain focus. But this can be customized using the `validationErrorVisibility` parameter of the `FeatureForm`. This property can be changed at any time to show all the errors. It supports two modes of visibility.

- **ValidationErrorVisibility.Automatic** : *Indicates that the validation errors are only visible for editable fields that have received focus.*
- **ValidationErrorVisibility.Visible** : *Indicates the validation is run for all the editable fields, and errors are displayed regardless of the focus state.*

```kotlin
@Composable
FeatureForm(  
    featureForm = featureForm,  
    modifier = Modifier.fillMaxSize(),  
    validationErrorVisibility = ValidationErrorVisibility.Visible  
)
```
*Note* : Once the `validationErrorVisibility`  is set to `Visible`, changing it back to `Automatic` will have no effect since all the fields have now gained focus to show any errors.

## Theming and Customization

`FeatureForm` uses the material3 theming system. The following `colors` and `typography` are used by the respective fields.

#### Text Fields
- Outline color - `MaterialTheme.colorScheme.outline`
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Input TextStyle - `MaterialTheme.typography.bodyLarge`
- SupportingText TextStyle - `MaterialTheme.typography.bodySmall`
- Error color  = `MaterialTheme.colorScheme.error`

#### Radio Buttons
- Outline color = `MaterialTheme.colorScheme.outline`
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Options TextStyle - `MaterialTheme.typography.bodyLarge`
- SupportingText TextStyle - `MaterialTheme.typography.bodySmall`
- Error color = `MaterialTheme.colorScheme.error`

#### Group Elements
- Outline Color - `MaterialTheme.colorScheme.outline`
- Header Color - `MaterialTheme.colorScheme.SurfaceVariant`
- Content background Color - `MaterialTheme.colorScheme.background`
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Description TextStyle - `MaterialTheme.typography.bodySmall`
