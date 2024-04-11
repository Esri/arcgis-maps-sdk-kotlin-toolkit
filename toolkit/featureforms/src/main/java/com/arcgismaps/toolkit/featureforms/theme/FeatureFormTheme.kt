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

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.GroupFormElement
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

/**
 * CompositionLocal used to pass a [FeatureFormColorScheme] down the tree.
 */
internal val LocalColorScheme: ProvidableCompositionLocal<FeatureFormColorScheme> =
    staticCompositionLocalOf {
        DefaultThemeTokens.colorScheme
    }

/**
 * CompositionLocal used to pass a [FeatureFormTypography] down the tree.
 */
internal val LocalTypography: ProvidableCompositionLocal<FeatureFormTypography> =
    staticCompositionLocalOf {
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
 * See [FeatureFormColorScheme.createDefaults] and [FeatureFormTypography.createDefaults] for the
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
    colorScheme: FeatureFormColorScheme = FeatureFormColorScheme.createDefaults(),
    typography: FeatureFormTypography = FeatureFormTypography.createDefaults(),
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
 * Use [FeatureFormColorScheme.createDefaults] to create a new instance with the default values.
 */
@Immutable
public data class FeatureFormColorScheme internal constructor(
    public val editableTextFieldColors: EditableTextFieldColors,
    public val readOnlyFieldColors: ReadOnlyFieldColors,
    public val radioButtonFieldColors: RadioButtonFieldColors,
    public val groupElementColors: GroupElementColors
) {

    public companion object {

        /**
         * Creates a [FeatureFormColorScheme] with default values.
         *
         * @param editableTextFieldColors The color scheme for the editable text field types.
         * @param readOnlyFieldColors The color scheme for the read-only field types.
         * @param radioButtonFieldColors The color scheme for the radio button field types.
         * @param groupElementColors The color scheme to use for any Group elements.
         */
        @Composable
        public fun createDefaults(
            editableTextFieldColors: EditableTextFieldColors = EditableTextFieldColors.createDefaults(),
            readOnlyFieldColors: ReadOnlyFieldColors = ReadOnlyFieldColors.createDefaults(),
            radioButtonFieldColors: RadioButtonFieldColors = RadioButtonFieldColors.createDefaults(),
            groupElementColors: GroupElementColors = GroupElementColors.createDefaults()
        ): FeatureFormColorScheme {
            return FeatureFormColorScheme(
                editableTextFieldColors = editableTextFieldColors,
                readOnlyFieldColors = readOnlyFieldColors,
                radioButtonFieldColors = radioButtonFieldColors,
                groupElementColors = groupElementColors
            )
        }
    }
}

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
 * Use [EditableTextFieldColors.createDefaults] to create a new instance with the default values.
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
 */
@Immutable
public data class EditableTextFieldColors internal constructor(
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
) {
    public companion object {

        /**
         * Creates an instance [EditableTextFieldColors] with default values from [MaterialTheme].
         *
         * @param focusedTextColor the color used for the input text of this text field when focused
         * @param unfocusedTextColor the color used for the input text of this text field when not focused
         * @param errorTextColor the color used for the input text of this text field when in error state
         * @param focusedContainerColor the container color for this text field when focused
         * @param unfocusedContainerColor the container color for this text field when not focused
         * @param errorContainerColor the container color for this text field when in error state
         * @param cursorColor the cursor color for this text field
         * @param errorCursorColor the cursor color for this text field when in error state
         * @param textSelectionColors the colors used when the input text of this text field is selected
         * @param focusedLeadingIconColor the leading icon color for this text field when focused
         * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
         * @param errorLeadingIconColor the leading icon color for this text field when in error state
         * @param focusedTrailingIconColor the trailing icon color for this text field when focused
         * @param unfocusedTrailingIconColor the trailing icon color for this text field when not focused
         * @param errorTrailingIconColor the trailing icon color for this text field when in error state
         * @param focusedLabelColor the label color for this text field when focused
         * @param unfocusedLabelColor the label color for this text field when not focused
         * @param errorLabelColor the label color for this text field when in error state
         * @param focusedPlaceholderColor the placeholder color for this text field when focused
         * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
         * @param errorPlaceholderColor the placeholder color for this text field when in error state
         * @param focusedSupportingTextColor the supporting text color for this text field when focused
         * @param unfocusedSupportingTextColor the supporting text color for this text field when not focused
         * @param errorSupportingTextColor the supporting text color for this text field when in error state
         * @param focusedPrefixColor the prefix color for this text field when focused
         * @param unfocusedPrefixColor the prefix color for this text field when not focused
         * @param errorPrefixColor the prefix color for this text field when in error state
         * @param focusedSuffixColor the suffix color for this text field when focused
         * @param unfocusedSuffixColor the suffix color for this text field when not focused
         * @param errorSuffixColor the suffix color for this text field when in error state
         */
        @Composable
        public fun createDefaults(
            focusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurface,
            errorTextColor: Color = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor: Color = Color.Transparent,
            unfocusedContainerColor: Color = Color.Transparent,
            errorContainerColor: Color = Color.Transparent,
            cursorColor: Color = MaterialTheme.colorScheme.primary,
            errorCursorColor: Color = MaterialTheme.colorScheme.error,
            textSelectionColors: TextSelectionColors = LocalTextSelectionColors.current,
            focusedIndicatorColor: Color = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorIndicatorColor: Color = MaterialTheme.colorScheme.error,
            focusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLeadingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorTrailingIconColor: Color = MaterialTheme.colorScheme.error,
            focusedLabelColor: Color = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor: Color = MaterialTheme.colorScheme.error,
            focusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            unfocusedPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            errorPlaceholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            focusedSupportingTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedSupportingTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorSupportingTextColor: Color = MaterialTheme.colorScheme.error,
            focusedPrefixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPrefixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorPrefixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedSuffixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedSuffixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            errorSuffixColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
        ): EditableTextFieldColors {
            return EditableTextFieldColors(
                focusedTextColor = focusedTextColor,
                unfocusedTextColor = unfocusedTextColor,
                errorTextColor = errorTextColor,
                focusedContainerColor = focusedContainerColor,
                unfocusedContainerColor = unfocusedContainerColor,
                errorContainerColor = errorContainerColor,
                cursorColor = cursorColor,
                errorCursorColor = errorCursorColor,
                textSelectionColors = textSelectionColors,
                focusedIndicatorColor = focusedIndicatorColor,
                unfocusedIndicatorColor = unfocusedIndicatorColor,
                errorIndicatorColor = errorIndicatorColor,
                focusedLeadingIconColor = focusedLeadingIconColor,
                unfocusedLeadingIconColor = unfocusedLeadingIconColor,
                errorLeadingIconColor = errorLeadingIconColor,
                focusedTrailingIconColor = focusedTrailingIconColor,
                unfocusedTrailingIconColor = unfocusedTrailingIconColor,
                errorTrailingIconColor = errorTrailingIconColor,
                focusedLabelColor = focusedLabelColor,
                unfocusedLabelColor = unfocusedLabelColor,
                errorLabelColor = errorLabelColor,
                focusedPlaceholderColor = focusedPlaceholderColor,
                unfocusedPlaceholderColor = unfocusedPlaceholderColor,
                errorPlaceholderColor = errorPlaceholderColor,
                focusedSupportingTextColor = focusedSupportingTextColor,
                unfocusedSupportingTextColor = unfocusedSupportingTextColor,
                errorSupportingTextColor = errorSupportingTextColor,
                focusedPrefixColor = focusedPrefixColor,
                unfocusedPrefixColor = unfocusedPrefixColor,
                errorPrefixColor = errorPrefixColor,
                focusedSuffixColor = focusedSuffixColor,
                unfocusedSuffixColor = unfocusedSuffixColor,
                errorSuffixColor = errorSuffixColor
            )
        }
    }
}

/**
 * Colors used for a [FieldFormElement] with any input type. If the [FieldFormElement.isEditable]
 * is false, it is rendered as a read-only field with colors specified by this class.
 *
 * Use [ReadOnlyFieldColors.createDefaults] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field
 * @property textColor the color used for the text of this field
 * @property supportingTextColor the color used for the supporting text of this field
 */
@Immutable
public data class ReadOnlyFieldColors internal constructor(
    public val labelColor: Color,
    public val textColor: Color,
    public val supportingTextColor: Color
) {
    public companion object {

        /**
         * Creates an instance of [ReadOnlyFieldColors] with default values from [MaterialTheme].
         *
         * @param labelColor the color used for the label of this field
         * @param textColor the color used for the text of this field
         * @param supportingTextColor the color used for the supporting text of this field
         */
        @Composable
        public fun createDefaults(
            labelColor: Color = Color.Unspecified,
            textColor: Color = Color.Unspecified,
            supportingTextColor: Color = Color.Unspecified
        ): ReadOnlyFieldColors {
            return ReadOnlyFieldColors(
                labelColor = labelColor,
                textColor = textColor,
                supportingTextColor = supportingTextColor
            )
        }
    }
}

/**
 * Colors that are used for a [GroupFormElement].
 *
 * Use [GroupElementColors.createDefaults] to create a new instance with the default values.
 *
 * @property labelColor the color used for the label of this field
 * @property supportingTextColor the color used for the supporting text of this field
 * @property outlineColor the color used for the outline of this field
 * @property headerColor the color used for the header of this field. The header contains
 * the label and supporting text of the field.
 * @property containerColor the color used for the container of this field. The container
 * contains the field elements.
 */
@Immutable
public data class GroupElementColors internal constructor(
    public val labelColor: Color,
    public val supportingTextColor: Color,
    public val outlineColor: Color,
    public val headerColor: Color,
    public val containerColor: Color
) {
    public companion object {

        /**
         * Creates an instance of [GroupElementColors] with default values from [MaterialTheme].
         *
         * @param labelColor the color used for the label of this field
         * @param supportingTextColor the color used for the supporting text of this field
         * @param outlineColor the color used for the outline of this field
         * @param headerColor the color used for the header of this field. The header contains
         * the label and supporting text of the field.
         * @param containerColor the color used for the container of this field. The container
         * contains the field elements.
         */
        @Composable
        public fun createDefaults(
            labelColor: Color = Color.Unspecified,
            supportingTextColor: Color = Color.Unspecified,
            outlineColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            headerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
            containerColor: Color = MaterialTheme.colorScheme.background
        ): GroupElementColors {
            return GroupElementColors(
                labelColor = labelColor,
                supportingTextColor = supportingTextColor,
                outlineColor = outlineColor,
                headerColor = headerColor,
                containerColor = containerColor

            )
        }
    }
}

/**
 * Colors used for a [FieldFormElement] with a [RadioButtonsFormInput].
 *
 * Use [RadioButtonFieldColors.createDefaults] to create a new instance with the default values.
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
 */
@Immutable
public data class RadioButtonFieldColors internal constructor(
    public val labelColor: Color,
    public val textColor: Color,
    public val supportingTextColor: Color,
    public val outlineColor: Color,
    public val selectedColor: Color,
    public val unselectedColor: Color,
    public val disabledSelectedColor: Color,
    public val disabledUnselectedColor: Color
) {
    public companion object {

        /**
         * Creates an instance of [RadioButtonFieldColors] with default values from [MaterialTheme].
         *
         * @param labelColor the color used for the label of this field
         * @param textColor the color used for the text of RadioButton
         * @param supportingTextColor the color used for the supporting text of this field
         * @param outlineColor the color used for the outline of this field
         * @param selectedColor the color to use for the RadioButton when selected and enabled.
         * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
         * @param disabledSelectedColor the color to use for the RadioButton when disabled and selected.
         * @param disabledUnselectedColor the color to use for the RadioButton when disabled and not
         * selected.
         */
        @Composable
        public fun createDefaults(
            labelColor: Color = Color.Unspecified,
            textColor: Color = Color.Unspecified,
            supportingTextColor: Color = Color.Unspecified,
            outlineColor: Color = MaterialTheme.colorScheme.outline,
            selectedColor: Color = MaterialTheme.colorScheme.primary,
            unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSelectedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledUnselectedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ): RadioButtonFieldColors {
            return RadioButtonFieldColors(
                labelColor = labelColor,
                textColor = textColor,
                supportingTextColor = supportingTextColor,
                outlineColor = outlineColor,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                disabledSelectedColor = disabledSelectedColor,
                disabledUnselectedColor = disabledUnselectedColor,
            )
        }
    }
}

/**
 * A Typography system for the [FeatureForm] built on top of [MaterialTheme]. This can be used to
 * style the text and labels of the form elements.
 *
 * Any nested elements within a GroupFormElement will also use the same typography style specified
 * as part of this class.
 *
 * Use [FeatureFormTypography.createDefaults] to create a new instance with the default values.
 *
 * @property editableTextFieldTypography The typography for the editable text field types.
 * @property readOnlyFieldTypography The typography for the read-only field types.
 * @property groupElementTypography The typography to use for any Group elements.
 * @property radioButtonFieldTypography The typography to use for the radio button field types.
 */
@Immutable
public class FeatureFormTypography internal constructor(
    public val editableTextFieldTypography: EditableTextFieldTypography,
    public val readOnlyFieldTypography: ReadOnlyFieldTypography,
    public val groupElementTypography: GroupElementTypography,
    public val radioButtonFieldTypography: RadioButtonFieldTypography
) {
    public companion object {

        /**
         * Creates a [FeatureFormTypography] with default values.
         *
         * @param editableTextFieldTypography The typography for the editable text field types.
         * @param readOnlyFieldTypography The typography for the read-only field types.
         * @param groupElementTypography The typography to use for any Group elements.
         * @param radioButtonFieldTypography The typography to use for the radio button field types.
         */
        @Composable
        public fun createDefaults(
            editableTextFieldTypography: EditableTextFieldTypography = EditableTextFieldTypography.createDefaults(),
            readOnlyFieldTypography: ReadOnlyFieldTypography = ReadOnlyFieldTypography.createDefaults(),
            groupElementTypography: GroupElementTypography = GroupElementTypography.createDefaults(),
            radioButtonFieldTypography: RadioButtonFieldTypography = RadioButtonFieldTypography.createDefaults()
        ): FeatureFormTypography {
            return FeatureFormTypography(
                editableTextFieldTypography = editableTextFieldTypography,
                readOnlyFieldTypography = readOnlyFieldTypography,
                groupElementTypography = groupElementTypography,
                radioButtonFieldTypography = radioButtonFieldTypography
            )
        }
    }
}

/**
 * Typography used for [FieldFormElement]s with input types [TextBoxFormInput], [TextAreaFormInput],
 * [DateTimePickerFormInput], [SwitchFormInput] and [ComboBoxFormInput].
 *
 * Use [EditableTextFieldTypography.createDefaults] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property textStyle The style for the text of this field
 * @property supportingTextStyle The style for the supporting text of this field
 */
@Immutable
public data class EditableTextFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {

        /**
         * Creates an instance of [EditableTextFieldTypography] with default values from [MaterialTheme].
         *
         * @param labelStyle The style for the label of this field
         * @param textStyle The style for the text of this field
         * @param supportingTextStyle The style for the supporting text of this field
         */
        @Composable
        public fun createDefaults(
            labelStyle: TextStyle = MaterialTheme.typography.bodySmall,
            textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
            supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
        ): EditableTextFieldTypography {
            return EditableTextFieldTypography(
                labelStyle = labelStyle,
                textStyle = textStyle,
                supportingTextStyle = supportingTextStyle

            )
        }
    }
}

/**
 * Typography used for a [FieldFormElement] with any input type. If the [FieldFormElement.isEditable]
 * is false, it is rendered as a read-only field with colors specified by this class.
 *
 * Use [ReadOnlyFieldTypography.createDefaults] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property textStyle The style for the text of this field
 * @property supportingTextStyle The style for the supporting text of this field
 */
@Immutable
public data class ReadOnlyFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {

        /**
         * Creates an instance of [ReadOnlyFieldTypography] with default values from [MaterialTheme].
         *
         * @param labelStyle The style for the label of this field
         * @param textStyle The style for the text of this field
         * @param supportingTextStyle The style for the supporting text of this field
         */
        @Composable
        public fun createDefaults(
            labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
            textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
            supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
        ): ReadOnlyFieldTypography {
            return ReadOnlyFieldTypography(
                labelStyle = labelStyle,
                textStyle = textStyle,
                supportingTextStyle = supportingTextStyle

            )
        }
    }
}

/**
 * Typography used for a [GroupFormElement].
 *
 * Use [GroupElementTypography.createDefaults] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property supportingTextStyle The style for the supporting text of this field
 */
@Immutable
public data class GroupElementTypography internal constructor(
    public val labelStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {

        /**
         * Creates an instance of [GroupElementTypography] with default values from [MaterialTheme].
         *
         * @param labelStyle The style for the label of this field
         * @param supportingTextStyle The style for the supporting text of this field
         */
        @Composable
        public fun createDefaults(
            labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
            supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
        ): GroupElementTypography {
            return GroupElementTypography(
                labelStyle = labelStyle,
                supportingTextStyle = supportingTextStyle
            )
        }
    }
}

/**
 * Typography used for a [FieldFormElement] with a [RadioButtonsFormInput].
 *
 * Use [RadioButtonFieldTypography.createDefaults] to create a new instance with the default values.
 *
 * @property labelStyle The style for the label of this field
 * @property optionStyle The style for the text of a RadioButton
 * @property supportingTextStyle The style for the supporting text of this field
 */
@Immutable
public data class RadioButtonFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val optionStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {

        /**
         * Creates an instance of [RadioButtonFieldTypography] with default values from [MaterialTheme].
         *
         * @param labelStyle The style for the label of this field
         * @param optionStyle The style for the text of a RadioButton
         * @param supportingTextStyle The style for the supporting text of this field
         */
        @Composable
        public fun createDefaults(
            labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
            optionStyle: TextStyle = MaterialTheme.typography.bodyLarge,
            supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
        ): RadioButtonFieldTypography {
            return RadioButtonFieldTypography(
                labelStyle = labelStyle,
                optionStyle = optionStyle,
                supportingTextStyle = supportingTextStyle
            )
        }
    }
}
