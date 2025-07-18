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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.arcgismaps.toolkit.featureforms.FeatureForm

/**
 * Contains the default values used by [FeatureForm].
 */
public object FeatureFormDefaults {

    /**
     * Creates a [FeatureFormColorScheme] with default values.
     *
     * @param editableTextFieldColors The color scheme for the editable text field types.
     * @param readOnlyFieldColors The color scheme for the read-only field types.
     * @param radioButtonFieldColors The color scheme for the radio button field types.
     * @param groupElementColors The color scheme to use for any Group elements.
     * @param attachmentsElementColors The color scheme to use for any Attachments elements.
     * @since 200.5.0
     */
    @Composable
    public fun colorScheme(
        editableTextFieldColors: EditableTextFieldColors = editableTextFieldColors(),
        readOnlyFieldColors: ReadOnlyFieldColors = readOnlyFieldColors(),
        radioButtonFieldColors: RadioButtonFieldColors = radioButtonFieldColors(),
        groupElementColors: GroupElementColors = groupElementColors(),
        attachmentsElementColors: AttachmentsElementColors = attachmentsElementColors()
    ): FeatureFormColorScheme {
        return FeatureFormColorScheme(
            editableTextFieldColors = editableTextFieldColors,
            readOnlyFieldColors = readOnlyFieldColors,
            radioButtonFieldColors = radioButtonFieldColors,
            groupElementColors = groupElementColors,
            attachmentsElementColors = attachmentsElementColors
        )
    }

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
     * @since 200.5.0
     */
    @Composable
    public fun editableTextFieldColors(
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

    /**
     * Creates an instance of [ReadOnlyFieldColors] with default values from [MaterialTheme].
     *
     * @param labelColor the color used for the label of this field.
     * @param textColor the color used for the text of this field.
     * @param supportingTextColor the color used for the supporting text of this field.
     * @param errorSupportingTextColor the color used for the supporting text of this field when in error state.
     * @since 200.5.0
     */
    @Composable
    public fun readOnlyFieldColors(
        labelColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
        supportingTextColor: Color = Color.Unspecified,
        errorSupportingTextColor: Color = MaterialTheme.colorScheme.error
    ): ReadOnlyFieldColors {
        return ReadOnlyFieldColors(
            labelColor = labelColor,
            textColor = textColor,
            supportingTextColor = supportingTextColor,
            errorSupportingTextColor = errorSupportingTextColor
        )
    }

    /**
     * Creates an instance of [GroupElementColors] with default values from [MaterialTheme].
     *
     * @param labelColor the color used for the label of this field.
     * @param supportingTextColor the color used for the supporting text of this field.
     * @param outlineColor the color used for the outline of this field.
     * @property containerColor the color used for the header container of this field. This contains
     * the label and supporting text of the field.
     * @property bodyColor the color used for the body of this field. The body
     * contains the field elements.
     * @since 200.5.0
     */
    @Composable
    public fun groupElementColors(
        labelColor: Color = Color.Unspecified,
        supportingTextColor: Color = Color.Unspecified,
        outlineColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        bodyColor: Color = MaterialTheme.colorScheme.background
    ): GroupElementColors {
        return GroupElementColors(
            labelColor = labelColor,
            supportingTextColor = supportingTextColor,
            outlineColor = outlineColor,
            containerColor = containerColor,
            bodyColor = bodyColor
        )
    }

    /**
     * Creates an instance of [RadioButtonFieldColors] with default values from [MaterialTheme].
     *
     * @param labelColor the color used for the label of this field.
     * @param textColor the color used for the text of RadioButton.
     * @param supportingTextColor the color used for the supporting text of this field.
     * @param outlineColor the color used for the outline of this field.
     * @param selectedColor the color to use for the RadioButton when selected and enabled.
     * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
     * @param disabledSelectedColor the color to use for the RadioButton when disabled and selected.
     * @param disabledUnselectedColor the color to use for the RadioButton when disabled and not
     * selected.
     * @since 200.5.0
     */
    @Composable
    public fun radioButtonFieldColors(
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

    /**
     * Creates an instance of [AttachmentsElementColors] with default values from [MaterialTheme].
     *
     * @param labelColor the color used for the label of this field.
     * @param supportingTextColor the color used for the supporting text of this field.
     * @param outlineColor the color used for the outline of this field.
     * @param containerColor the color used for the container of this field.
     * @param tileTextColor the color used for the text of the individual attachment tile.
     * @param tileBorderColor the color used for the border of the individual attachment tile.
     * @param scrollBarColor the color used for the scroll bar in the attachment list.
     */
    @Composable
    public fun attachmentsElementColors(
        labelColor: Color = Color.Unspecified,
        supportingTextColor: Color = Color.Unspecified,
        outlineColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        containerColor: Color = Color.Unspecified,
        tileTextColor: Color = MaterialTheme.colorScheme.onSurface,
        tileBorderColor: Color = MaterialTheme.colorScheme.outline,
        tileContainerColor: Color = Color.Unspecified,
        scrollBarColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    ): AttachmentsElementColors {
        return AttachmentsElementColors(
            labelColor = labelColor,
            supportingTextColor = supportingTextColor,
            outlineColor = outlineColor,
            containerColor = containerColor,
            tileTextColor = tileTextColor,
            tileBorderColor = tileBorderColor,
            tileContainerColor = tileContainerColor,
            scrollBarColor = scrollBarColor
        )
    }

    /**
     * Creates a [FeatureFormTypography] with default values.
     *
     * @param editableTextFieldTypography The typography for the editable text field types.
     * @param readOnlyFieldTypography The typography for the read-only field types.
     * @param groupElementTypography The typography to use for any Group elements.
     * @param radioButtonFieldTypography The typography to use for the radio button field types.
     * @param attachmentsElementTypography The typography to use for any Attachments elements.
     * @since 200.5.0
     */
    @Composable
    public fun typography(
        editableTextFieldTypography: EditableTextFieldTypography = editableTextFieldTypography(),
        readOnlyFieldTypography: ReadOnlyFieldTypography = readOnlyFieldTypography(),
        groupElementTypography: GroupElementTypography = groupElementTypography(),
        radioButtonFieldTypography: RadioButtonFieldTypography = radioButtonFieldTypography(),
        attachmentsElementTypography: AttachmentsElementTypography = attachmentsElementTypography()
    ): FeatureFormTypography {
        return FeatureFormTypography(
            editableTextFieldTypography = editableTextFieldTypography,
            readOnlyFieldTypography = readOnlyFieldTypography,
            groupElementTypography = groupElementTypography,
            radioButtonFieldTypography = radioButtonFieldTypography,
            attachmentsElementTypography = attachmentsElementTypography
        )
    }

    /**
     * Creates an instance of [EditableTextFieldTypography] with default values from [MaterialTheme].
     *
     * @param labelStyle The style for the label of this field.
     * @param textStyle The style for the text of this field.
     * @param supportingTextStyle The style for the supporting text of this field.
     * @since 200.5.0
     */
    @Composable
    public fun editableTextFieldTypography(
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

    /**
     * Creates an instance of [ReadOnlyFieldTypography] with default values from [MaterialTheme].
     *
     * @param labelStyle The style for the label of this field.
     * @param textStyle The style for the text of this field.
     * @param supportingTextStyle The style for the supporting text of this field.
     * @since 200.5.0
     */
    @Composable
    public fun readOnlyFieldTypography(
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

    /**
     * Creates an instance of [GroupElementTypography] with default values from [MaterialTheme].
     *
     * @param labelStyle The style for the label of this field.
     * @param supportingTextStyle The style for the supporting text of this field.
     * @since 200.5.0
     */
    @Composable
    public fun groupElementTypography(
        labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
        supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
    ): GroupElementTypography {
        return GroupElementTypography(
            labelStyle = labelStyle,
            supportingTextStyle = supportingTextStyle
        )
    }

    /**
     * Creates an instance of [RadioButtonFieldTypography] with default values from [MaterialTheme].
     *
     * @param labelStyle The style for the label of this field.
     * @param optionStyle The style for the text of a RadioButton.
     * @param supportingTextStyle The style for the supporting text of this field.
     * @since 200.5.0
     */
    @Composable
    public fun radioButtonFieldTypography(
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

    /**
     * Creates an instance of [AttachmentsElementTypography] with default values from [MaterialTheme].
     *
     * @param labelStyle The style for the label of this field.
     * @param supportingTextStyle The style for the supporting text of this field.
     * @param tileTextStyle The style for the text of the individual attachment tile.
     * @since 200.5.0
     */
    @Composable
    public fun attachmentsElementTypography(
        labelStyle: TextStyle = MaterialTheme.typography.titleLarge,
        supportingTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
        tileTextStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(
            textAlign = TextAlign.Center
        ),
        tileSupportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
    ): AttachmentsElementTypography {
        return AttachmentsElementTypography(
            labelStyle = labelStyle,
            supportingTextStyle = supportingTextStyle,
            tileTextStyle = tileTextStyle,
            tileSupportingTextStyle = tileSupportingTextStyle
        )
    }
}
