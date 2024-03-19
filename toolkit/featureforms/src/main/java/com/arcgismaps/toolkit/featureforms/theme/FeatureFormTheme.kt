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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

internal val LocalFeatureFormTheme: ProvidableCompositionLocal<FeatureFormTheme> =
    staticCompositionLocalOf {
        defaultTheme()
    }

/**
 * Creates a default theme from statically defined tokens. Do not use this at runtime, instead the
 * FeatureFormTheme must always be specified from the current MaterialTheme.
 *
 * This mainly provides a default theme for use within previews.
 */
private fun defaultTheme(): FeatureFormTheme =
    FeatureFormTheme(
        colorScheme = DefaultThemeTokens.colorScheme,
        typography = DefaultThemeTokens.typography
    )


@Composable
internal fun FeatureFormTheme(
    theme: FeatureFormTheme = FeatureFormTheme.createDefaults(),
    content: @Composable () -> Unit
) {
    val rememberedTheme = remember { theme }
    CompositionLocalProvider(LocalFeatureFormTheme provides rememberedTheme) {
        content()
    }
}

/**
 * If a type has both custom typography with color and a custom color specified, the color
 * specified will take precedence. This color will be merged with the text style.
 */
public data class FeatureFormTheme internal constructor(
    val colorScheme: FeatureFormColorScheme,
    val typography: FeatureFormTypography
) {
    public companion object {
        @Composable
        public fun createDefaults(
            colorScheme: FeatureFormColorScheme = FeatureFormColorScheme.createDefaults(),
            typography: FeatureFormTypography = FeatureFormTypography.createDefaults()
        ): FeatureFormTheme {
            return FeatureFormTheme(
                colorScheme = colorScheme,
                typography = typography
            )
        }
    }
}

public data class FeatureFormColorScheme internal constructor(
    public val editableTextFieldColors: EditableTextFieldColors,
    public val readOnlyTextFieldColors: ReadOnlyTextFieldColors,
    public val radioButtonFieldColors: RadioButtonFieldColors,
    public val groupElementColors: GroupElementColors
) {

    public companion object {
        @Composable
        public fun createDefaults(
            editableTextFieldColors: EditableTextFieldColors = EditableTextFieldColors.createDefaults(),
            readOnlyTextFieldColors: ReadOnlyTextFieldColors = ReadOnlyTextFieldColors.createDefaults(),
            radioButtonFieldColors: RadioButtonFieldColors = RadioButtonFieldColors.createDefaults(),
            groupElementColors: GroupElementColors = GroupElementColors.createDefaults()
        ): FeatureFormColorScheme {
            return FeatureFormColorScheme(
                editableTextFieldColors = editableTextFieldColors,
                readOnlyTextFieldColors = readOnlyTextFieldColors,
                radioButtonFieldColors = radioButtonFieldColors,
                groupElementColors = groupElementColors
            )
        }
    }
}

public class FeatureFormTypography internal constructor(
    public val editableTextFieldTypography: EditableTextFieldTypography,
    public val readOnlyTextFieldTypography: ReadOnlyTextFieldTypography,
    public val groupElementTypography: GroupElementTypography,
    public val radioButtonFieldTypography: RadioButtonFieldTypography
) {
    public companion object {
        @Composable
        public fun createDefaults(
            editableTextFieldTypography: EditableTextFieldTypography = EditableTextFieldTypography.createDefaults(),
            readOnlyTextFieldTypography: ReadOnlyTextFieldTypography = ReadOnlyTextFieldTypography.createDefaults(),
            groupElementTypography: GroupElementTypography = GroupElementTypography.createDefaults(),
            radioButtonFieldTypography: RadioButtonFieldTypography = RadioButtonFieldTypography.createDefaults()
        ): FeatureFormTypography {
            return FeatureFormTypography(
                editableTextFieldTypography = editableTextFieldTypography,
                readOnlyTextFieldTypography = readOnlyTextFieldTypography,
                groupElementTypography = groupElementTypography,
                radioButtonFieldTypography = radioButtonFieldTypography
            )
        }
    }
}

public data class EditableTextFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {
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

public data class ReadOnlyTextFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val textStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {
        @Composable
        public fun createDefaults(
            labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
            textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
            supportingTextStyle: TextStyle = MaterialTheme.typography.bodySmall
        ): ReadOnlyTextFieldTypography {
            return ReadOnlyTextFieldTypography(
                labelStyle = labelStyle,
                textStyle = textStyle,
                supportingTextStyle = supportingTextStyle

            )
        }
    }
}

public data class GroupElementTypography internal constructor(
    public val labelStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {
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

public data class RadioButtonFieldTypography internal constructor(
    public val labelStyle: TextStyle,
    public val optionStyle: TextStyle,
    public val supportingTextStyle: TextStyle
) {
    public companion object {
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

public data class ReadOnlyTextFieldColors(
    public val labelColor: Color,
    public val textColor: Color,
    public val supportingTextColor: Color
) {
    public companion object {
        @Composable
        public fun createDefaults(
            labelColor: Color = Color.Unspecified,
            textColor: Color = Color.Unspecified,
            supportingTextColor: Color = Color.Unspecified
        ): ReadOnlyTextFieldColors {
            return ReadOnlyTextFieldColors(
                labelColor = labelColor,
                textColor = textColor,
                supportingTextColor = supportingTextColor
            )
        }
    }
}

public data class GroupElementColors(
    public val labelColor: Color,
    public val supportingTextColor: Color,
    public val outlineColor: Color,
    public val headerColor: Color,
    public val containerColor: Color
) {
    public companion object {
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
