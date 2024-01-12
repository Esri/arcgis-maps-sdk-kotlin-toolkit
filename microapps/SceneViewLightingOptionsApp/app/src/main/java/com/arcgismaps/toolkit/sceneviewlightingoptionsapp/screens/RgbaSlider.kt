package com.arcgismaps.toolkit.sceneviewlightingoptionsapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Displays a slider with a range of 0 to 255, set to the initial [value].
 */
@Composable
fun RgbaSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(0.25f))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            modifier = Modifier.weight(0.5f, true),
            valueRange = 0f..255f,
            steps = 255
        )
        Text(text = value.toString(), Modifier.weight(0.25f), textAlign = TextAlign.End)
    }
}
