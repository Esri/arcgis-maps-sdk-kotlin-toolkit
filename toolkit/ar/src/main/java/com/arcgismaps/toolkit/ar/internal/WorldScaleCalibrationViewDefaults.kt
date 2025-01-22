package com.arcgismaps.toolkit.ar.internal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

public object WorldScaleCalibrationViewDefaults {

    @Composable
    internal fun colorScheme(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        closeButtonContainerColor: Color = MaterialTheme.colorScheme.secondary,
        closeButtonContentColor: Color = MaterialTheme.colorScheme.onSecondary,
        buttonContainerColor: Color = MaterialTheme.colorScheme.primary,
        buttonContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        sliderTrackColor: Color = MaterialTheme.colorScheme.primary,
        sliderThumbColor: Color = MaterialTheme.colorScheme.onPrimary
    ): WorldScaleCalibrationViewColorScheme {
        return WorldScaleCalibrationViewColorScheme(
            backgroundColor = backgroundColor,
            containerColor = containerColor,
            closeButtonContainerColor = closeButtonContainerColor,
            closeButtonContentColor = closeButtonContentColor,
            buttonContainerColor = buttonContainerColor,
            buttonContentColor = buttonContentColor,
            sliderTrackColor = sliderTrackColor,
            sliderThumbColor = sliderThumbColor
        )
    }

    @Composable
    internal fun typography(
        titleTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.secondary
        ),
        subtitleTextStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary
        ),
        bodyTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.primary
        )
    ): WorldScaleCalibrationViewTypography {
        return WorldScaleCalibrationViewTypography(
            titleTextStyle = titleTextStyle,
            subtitleTextStyle = subtitleTextStyle,
            bodyTextStyle = bodyTextStyle
        )
    }
}