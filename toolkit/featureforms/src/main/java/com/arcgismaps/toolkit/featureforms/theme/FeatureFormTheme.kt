/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.FeatureForm

/**
 * CompositionLocal used to pass a [FeatureFormColorScheme] down the tree.
 */
internal val LocalColorScheme: ProvidableCompositionLocal<FeatureFormColorScheme> =
    compositionLocalOf {
        DefaultThemeTokens.colorScheme
    }

/**
 * CompositionLocal used to pass a [FeatureFormTypography] down the tree.
 */
internal val LocalTypography: ProvidableCompositionLocal<FeatureFormTypography> =
    compositionLocalOf {
        DefaultThemeTokens.typography
    }

/**
 * Provides compose functions to access the current theme values.
 */
internal object FeatureFormTheme {

    /**
     * Retrieves the current [FeatureFormColorScheme].
     */
    val colorScheme: FeatureFormColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current

    /**
     * Retrieves the current [FeatureFormTypography].
     */
    val typography: FeatureFormTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

/**
 * Provides a default [FeatureFormTheme] to the given [content] so that the FeatureForm can be
 * customized.
 *
 * The default value for the [colorScheme] and [typography] is based on the current [MaterialTheme].
 * See [FeatureFormDefaults.colorScheme] and [FeatureFormDefaults.typography] for the
 * exact configuration used.
 *
 * @param colorScheme A [FeatureFormColorScheme] to use for this compose hierarchy
 * @param typography A [FeatureFormTypography] to use for this compose hierarchy
 *
 * A complete definition for the [FeatureFormTheme] to use. A default is provided based
 * on the current [MaterialTheme].
 * @param content The content to which the theme should be applied.
 */
@Composable
internal fun FeatureFormTheme(
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalTypography provides typography
    ) {
        content()
    }
}

/**
 * A color scheme that holds all the color parameters for a [FeatureForm].
 *
 * The scheme provides default values for all colors as a starting point for customization. These
 * defaults are populated using [MaterialTheme].
 *
 * Any nested elements within a GroupFormElement will also use the same color scheme specified as
 * part of this class.
 *
 *
 * Use [FeatureFormDefaults.colorScheme] to create a new instance with the default values.
 *
 * @property editableTextFieldColors The color scheme for the editable text field types.
 * @property readOnlyFieldColors The color scheme for the read-only field types.
 * @property radioButtonFieldColors The color scheme for the radio button field types.
 * @property groupElementColors The color scheme to use for any Group elements.
 * @property attachmentsElementColors The color scheme to use for any Attachments elements.
 * @since 200.5.0
 */
@Immutable
public data class FeatureFormColorScheme(
    public val editableTextFieldColors: EditableTextFieldColors,
    public val readOnlyFieldColors: ReadOnlyFieldColors,
    public val radioButtonFieldColors: RadioButtonFieldColors,
    public val groupElementColors: GroupElementColors,
    public val attachmentsElementColors: AttachmentsElementColors
)

/**
 * Colors used for [FieldFormElement]s with input types [TextBoxFormInput], [TextAreaFormInput],
 * [DateTimePickerFormInput], [SwitchFormInput] and [ComboBoxFormInput].
 *
 * Since the FeatureForm uses an [OutlinedTextField], the default values are borrowed from
 * [OutlinedTextFieldDefaults.colors].
 *
 * Note that this does not provide disabled colors, since read-only fields are rendered using
 * [ReadOnlyFieldColors].
 *
 * Use [FeatureFormDefaults.editableTextFieldColors] to create a new instance with the default values.
 *
 * @property focusedTextColor the color used for the input text of this text field when focused
 * @property unfocusedTextColor the color used for the input text of this text field when not focused
 * @property errorTextColor the color used for the input text of this text field when in error state
 * @property focusedContainerColor the container color for this text field when focused
 * @property unfocusedContainerColor the container color for this text field when not focused
 * @property errorContainerColor the container color for this text field when in error state
 * @property cursorColor the cursor color for this text field
 * @property errorCursorColor the cursor color for this text field when in error state
 * @property textSelectionColors the colors used when the input text of this text field is selected
 * @property focusedLeadingIconColor the leading icon color for this text field when focused
 * @property unfocusedLeadingIconColor the leading icon color for this text field when not focused
 * @property errorLeadingIconColor the leading icon color for this text field when in error state
 * @property focusedTrailingIconColor the trailing icon color for this text field when focused
 * @property unfocusedTrailingIconColor the trailing icon color for this text field when not focused
 * @property errorTrailingIconColor the trailing icon color for this text field when in error state
 * @property focusedLabelColor the label color for this text field when focused
 * @property unfocusedLabelColor the label color for this text field when not focused
 * @property errorLabelColor the label color for this text field when in error state
 * @property focusedPlaceholderColor the placeholder color for this text field when focused
 * @property unfocusedPlaceholderColor the placeholder color for this text field when not focused
 * @property errorPlaceholderColor the placeholder color for this text field when in error state
 * @property focusedSupportingTextColor the supporting text color for this text field when focused
 * @property unfocusedSupportingTextColor the supporting text color for this text field when not focused
 * @property errorSupportingTextColor the supporting text color for this text field when in error state
 * @property focusedPrefixColor the prefix color for this text field when focused
 * @property unfocusedPrefixColor the prefix color for this text field when not focused
 * @property errorPrefixColor the prefix color for this text field when in error state
 * @property focusedSuffixColor the suffix color for this text field when focused
 * @property unfocusedSuffixColor the suffix color for this text field when not focused
 * @property errorSuffixColor the suffix color for this text field when in error state
 * @since 200.5.0
 */
@Immutable
public data class EditableTextFieldColors(
    public val focusedTextColor: Color,
    public val unfocusedTextColor: Color,
    public val errorTextColor: Color,
    public val focusedContainerColor: Color,
    public val unfocusedContainerColor: Color,
    public val errorContainerColor: Color,
    public val cursorColor: Color,
    public val errorCursorColor: Color,
    public val textSelectionColors: TextSelectionColors,
    public val focusedIndicatorColor: Color,
    public val unfocusedIndicatorColor: Color,
    public val errorIndicatorColor: Color,
    public val focusedLeadingIconColor: Color,
    public val unfocusedLeadingIconColor: Color,
    public val errorLeadingIconColor: Color,
    public val focusedTrailingIconColor: Color,
    public val unfocusedTrailingIconColor: Color,
    public val errorTrailingIconColor: Color,
    public val focusedLabelColor: Color,
    public val unfocusedLabelColor: Color,
    public val errorLabelColor: Color,
    public val focusedPlaceholderColor: Color,
    public val unfocusedPlaceholderColor: Color,
    public val errorPlaceholderColor: Color,
    public val focusedSupportingTextColor: Color,
    public val unfocusedSupportingTextColor: Color,
    public val errorSupportingTextColor: Color,
    public val focusedPrefixColor: Color,
    public val unfocusedPrefixColor: Color,
    public val errorPrefixColor: Color,
    public val focusedSuffixColor: Color,
    public val unfocusedSuffixColor: Color,
    public val errorSuffixColor: Color,
)

/**
 * Colors used for a [FieldFormElement] with any input type. If the [FieldFormElement.isEditable]
 * is false, it is rendered as a read-only field with colors specified by this class.
 *
 * Use [FeatureFormDefaults.readOnlyFieldColors] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field
 * @property textColor the color used for the text of this field
 * @property supportingTextColor the color used for the supporting text of this field
 * @property errorSupportingTextColor the color used for the supporting text of this field when in error state
 * @since 200.5.0
 */
@Immutable
public data class ReadOnlyFieldColors(
    public val labelColor: Color,
    public val textColor: Color,
    public val supportingTextColor: Color,
    public val errorSupportingTextColor: Color
)

/**
 * Colors that are used for a [GroupFormElement].
 *
 * Use [FeatureFormDefaults.groupElementColors] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field
 * @property supportingTextColor the color used for the supporting text of this field
 * @property outlineColor the color used for the outline of this field
 * @property containerColor the color used for the header container of this field. This contains
 * the label and supporting text of the field.
 * @property bodyColor the color used for the body of this field. The body
 * contains the field elements.
 * @since 200.5.0
 */
@Immutable
public data class GroupElementColors(
    public val labelColor: Color,
    public val supportingTextColor: Color,
    public val outlineColor: Color,
    public val containerColor: Color,
    public val bodyColor: Color
)

/**
 * Colors used for a [FieldFormElement] with a [RadioButtonsFormInput].
 *
 * Use [FeatureFormDefaults.radioButtonFieldColors] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field
 * @property textColor the color used for the text of RadioButton
 * @property supportingTextColor the color used for the supporting text of this field
 * @property outlineColor the color used for the outline of this field
 * @property selectedColor the color to use for the RadioButton when selected and enabled.
 * @property unselectedColor the color to use for the RadioButton when unselected and enabled.
 * @property disabledSelectedColor the color to use for the RadioButton when disabled and selected.
 * @property disabledUnselectedColor the color to use for the RadioButton when disabled and not
 * selected.
 * @since 200.5.0
 */
@Immutable
public data class RadioButtonFieldColors(
    public val labelColor: Color,
    public val textColor: Color,
    public val supportingTextColor: Color,
    public val outlineColor: Color,
    public val selectedColor: Color,
    public val unselectedColor: Color,
    public val disabledSelectedColor: Color,
    public val disabledUnselectedColor: Color
)

/**
 * Colors used for a [AttachmentsFormElement]. Attachments in the element are rendered as a list
 * of tiles.
 *
 * Use [FeatureFormDefaults.attachmentsElementColors] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field.
 * @property supportingTextColor the color used for the supporting text of this field.
 * @property outlineColor the color used for the outline of this field.
 * @property containerColor the color used for the container of this field.
 * @property tileTextColor the color used for the text of an individual attachment tile.
 * @property tileBorderColor the color used for the border of an individual attachment tile.
 * @property scrollBarColor the color used for the scroll bar of the attachment list.
 * @since 200.5.0
 */
public data class AttachmentsElementColors(
    public val labelColor: Color,
    public val supportingTextColor: Color,
    @Deprecated("outlineColor is deprecated since the element is no longer outlined")
    public val outlineColor: Color,
    public val containerColor: Color,
    public val tileTextColor: Color,
    @Deprecated(
        message = "tileBorderColor is deprecated since the tile is no longer outlined",
        replaceWith = ReplaceWith("tileContainerColor")
    )
    public val tileBorderColor: Color,
    public val tileContainerColor: Color,
    public val scrollBarColor: Color,
)

/**
 * A Typography system for the [FeatureForm] built on top of [MaterialTheme]. This can be used to
 * style the text and labels of the form elements.
 *
 * Any nested elements within a GroupFormElement will also use the same typography style specified
 * as part of this class.
 *
 * Use [FeatureFormDefaults.typography] to create a new instance with the default values.
 *
 * @property editableTextFieldTypography The typography for the editable text field types.
 * @property readOnlyFieldTypography The typography for the read-only field types.
 * @property groupElementTypography The typography to use for any Group elements.
 * @property radioButtonFieldTypography The typography to use for the radio button field types.
 * @property attachmentsElementTypography The typography to use for any Attachments elements.
 * @since 200.5.0
 */
@Immutable
public data class FeatureFormTypography(
    public val editableTextFieldTypography: EditableTextFieldTypography,
    public val readOnlyFieldTypography: ReadOnlyFieldTypography,
    public val groupElementTypography: GroupElementTypography,
    public val radioButtonFieldTypography: RadioButtonFieldTypography,
    public val attachmentsElementTypography: AttachmentsElementTypography
)

/**
 * Typography used for [FieldFormElement]s with input types [TextBoxFormInput], [TextAreaFormInput],
 * [DateTimePickerFormInput], [SwitchFormInput] and [ComboBoxFormInput].
 *
 * Use [FeatureFormDefaults.editableTextFieldTypography] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property textStyle The style for the text of this field
 * @property supportingTextStyle The style for the supporting text of this field
 * @since 200.5.0
 */
@Immutable
public data class EditableTextFieldTypography(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
)

/**
 * Typography used for a [FieldFormElement] with any input type. If the [FieldFormElement.isEditable]
 * is false, it is rendered as a read-only field with colors specified by this class.
 *
 * Use [FeatureFormDefaults.readOnlyFieldTypography] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property textStyle The style for the text of this field
 * @property supportingTextStyle The style for the supporting text of this field
 * @since 200.5.0
 */
@Immutable
public data class ReadOnlyFieldTypography(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
)

/**
 * Typography used for a [GroupFormElement].
 *
 * Use [FeatureFormDefaults.groupElementTypography] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property supportingTextStyle The style for the supporting text of this field
 * @since 200.5.0
 */
@Immutable
public data class GroupElementTypography(
    public val labelStyle: TextStyle,
    public val supportingTextStyle: TextStyle
)

/**
 * Typography used for a [FieldFormElement] with a [RadioButtonsFormInput].
 *
 * Use [FeatureFormDefaults.radioButtonFieldTypography] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property optionStyle The style for the text of a RadioButton
 * @property supportingTextStyle The style for the supporting text of this field
 * @since 200.5.0
 */
@Immutable
public data class RadioButtonFieldTypography(
    public val labelStyle: TextStyle,
    public val optionStyle: TextStyle,
    public val supportingTextStyle: TextStyle
)

/**
 * Typography used for a [AttachmentsFormElement]. Attachments in the element are rendered as a list
 * of tiles.
 *
 * Use [FeatureFormDefaults.attachmentsElementTypography] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field.
 * @property supportingTextStyle The style for the supporting text of this field.
 * @property tileTextStyle The style for the text of an individual attachment tile.
 * @since 200.5.0
 */
public data class AttachmentsElementTypography(
    public val labelStyle: TextStyle,
    public val supportingTextStyle: TextStyle,
    public val tileTextStyle: TextStyle,
)
