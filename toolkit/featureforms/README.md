

# FeatureForm

## Description

The `FeatureForm` toolkit component enables users to display and edit feature attributes and attachments in a layer using the `FeatureForm` API.
FeatureForms can be authored as part of the web map using [Field Maps Designer](https://www.arcgis.com/apps/fieldmaps/) or using Map Viewer. This allows a simplified user experience to edit feature attribute data on the web map.

More information about edit attributes using forms can be found [here](https://next.sites.afd.arcgis.com/kotlin/edit-features/edit-attribute-values/).

## Features

The `FeatureForm` is a composable that can render a `FeatureForm` object with a `FeatureFormDefinition`  using Jetpack Compose.
- It can be integrated into any custom layout or container.
- All expressions present in the form are initially evaluated with a progress indicator before the FeatureForm is available for editing.
- Provides automatic saving and restoring of form data after device configuration changes.
- Provides a DateTime picker and a picker for coded-value field types.
- Shows validation errors for any fields with errors.
- Visibility behavior of validation errors can be customized. See [Changing the Validation Error Visibility policy](#changing-the-validation-error-visibility-policy).
- Follows material 3 design system.

## Usage

To see it in action, check out the [microapp](../../microapps/FeatureFormsApp). The micro-app integrates the `FeatureForm` component into a `BottomSheet`.

*View the API Reference for the `featureforms` module [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.featureforms/index.html).*

A `FeatureForm` composable can be created using a `FeatureForm` object as follows.

#### Creating a FeatureForm object

```kotlin
// create the FeatureForm from the Feature
val featureForm = FeatureForm(feature, featureFormDefinition)
```

#### Creating a FeatureForm UI in Compose

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

### Permissions

The `FeatureForm` component requires the following permissions to function correctly. These permissions are added to the AndroidManifest.xml file of the library. 
But the calling app should request these permissions at runtime. If the permissions are not granted, the respective functionality will be limited.

- `Manifest.permission.CAMERA` : Required to use the camera for the `BarcodeScannerFormInput` input type and the `AttachmentsFormElement` to add attachments.

## Behavior

- The `FeatureForm` composable will display the form fields in the order they are defined in the `FeatureFormDefinition`.
- The component supports the following [FormElement](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/-form-element/index.html?query=sealed%20class%20FormElement) types as part of its configuration.
  
### [AttachmentsFormElement](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/-attachments-form-element/index.html?query=class%20AttachmentsFormElement%20:%20FormElement)

- The `AttachmentsFormElement` is used to display and manage attachments for a feature. It allows users to add, view, and delete attachments.
- If camera permissions are not granted, the "Add" button will not be visible.

<img src="screenshots/attachments_form_element.png" width="250"/>
  
### [FieldFormElement](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#416104072%2FClasslikes%2F-1844196645) with the following [FormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#902783574%2FClasslikes%2F-1844196645) types -

#### [BarcodeScannerFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#1532812564%2FClasslikes%2F-1844196645)

- The `BarcodeScannerFormInput` is a type of text input that also allows users to scan barcodes using the device's camera.
- Tapping on the barcode icon will open the camera to scan a barcode.
- If camera permissions are not granted, the barcode scanner will not be available, and the input will behave like a normal text input.

<img src="screenshots/barcode_input.png" width="250"/>

#### [ComboBoxFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-3732077%2FClasslikes%2F-1844196645)

- This input type is used for fields with a coded value domain.
- The user can select a value from a list of predefined values using the picker.

<img src="screenshots/combo_input.png" width="250"/>
<img src="screenshots/combo_picker.png" height="75"/>

#### [DateTimePickerFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-399367937%2FClasslikes%2F-1844196645)

- This input type is used for fields with a date-time or date-only data type.
- The user can select a date and time using the date-time picker. 
- The [date](https://m3.material.io/components/date-pickers/overview) and [time](https://m3.material.io/components/time-pickers/overview) picker are based on the material3 design system.

<img src="screenshots/date_time_input.png" width="250"/>

#### [RadioButtonsFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-802642052%2FClasslikes%2F-1844196645)

- This input type provides a radio button group for fields with a coded value domain.
- If the current value is not one of the possible values, it is displayed as a `ComboBoxFormInput`.

<img src="screenshots/radio_input.png" width="250"/>

#### [SwitchFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-810335126%2FClasslikes%2F-1844196645)

- This input type provides a switch for fields with only two possible coded values.
- If the current value is not one of the two possible values, the switch is displayed as a `ComboBoxFormInput`.

<img src="screenshots/switch_input.png" width="250"/>

#### [TextAreaFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#1246148688%2FClasslikes%2F-1844196645)

- Provides a multi-line text input field.

<img src="screenshots/text_area_input.png" width="250"/>

#### [TextBoxFormInput](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-2106495906%2FClasslikes%2F-1844196645)

- Provides a single-line text input field.

<img src="screenshots/text_input.png" width="250"/>

#### Read-Only Fields

- Any field that is read-only as indicated by the [isEditable](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/-field-form-element/index.html#279696009%2FProperties%2F-1844196645) property will be displayed as read-only field with
special styling.
- If the field is backed by an arcade expression as indicated by [hasValueExpression](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/-field-form-element/index.html#854459496%2FProperties%2F-1844196645), the icon `<>` will be displayed next to the field.

<img src="screenshots/read_only.png" width="250"/>
  
### [GroupFormElement](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.mapping.featureforms/index.html#-414583411%2FClasslikes%2F-1844196645)

- The `GroupFormElement` is used to group multiple `FieldFormElement`s together.
- The group can be collapsed or expanded by tapping on the header.

<img src="screenshots/group_element_collapsed.png" width="250"/>

<img src="screenshots/group_element_expanded.png" width="250"/>

### TextFormElement

- The `TextFormElement` is used to display text in the form.
- It supports plain text and markdown text.

<img src="screenshots/text_form_element.png" width="250"/>

## Theming and Customization

`FeatureForm` uses the material3 theming system. These styles and colors are customizable through the `FeatureForms` composable function's public API. The following `colors` and `typography` are used by the respective fields.

#### Text Fields
- Outline color - `MaterialTheme.colorScheme.outline`
- Label TextStyle - `MaterialTheme.typography.bodySmall`
- Input TextStyle - `MaterialTheme.typography.bodyLarge`
- SupportingText TextStyle - `MaterialTheme.typography.bodySmall`
- Error color - `MaterialTheme.colorScheme.error`

#### Radio Buttons
- Outline color - `MaterialTheme.colorScheme.outline`
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Options TextStyle - `MaterialTheme.typography.bodyLarge`
- SupportingText TextStyle - `MaterialTheme.typography.bodySmall`
- Error color - `MaterialTheme.colorScheme.error`

#### Group Elements
- Outline Color - `MaterialTheme.colorScheme.outline`
- Header Color - `MaterialTheme.colorScheme.SurfaceVariant`
- Content background Color - `MaterialTheme.colorScheme.background`
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Description TextStyle - `MaterialTheme.typography.bodySmall`

#### Read-Only Fields
- Label TextStyle - `MaterialTheme.typography.bodyMedium`
- Input TextStyle - `MaterialTheme.typography.bodyLarge`
- SupportingText TextStyle - `MaterialTheme.typography.bodySmall`

More information on the material 3 specs [here](https://m3.material.io/components/text-fields/specs#e4964192-72ad-414f-85b4-4b4357abb83c)
