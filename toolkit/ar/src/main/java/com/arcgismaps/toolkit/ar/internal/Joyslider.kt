/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.ar.internal

import android.icu.text.DecimalFormat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

/**
 * Creates a Joyslider that follows user input like a normal slider, but returns to zero input
 * when released. When held, [onValueChange] is triggered at a regular interval even if the
 * value is unchanged.
 *
 * @since 200.7.0
 */
@Composable
internal fun Joyslider(
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var userInteracting by remember { mutableStateOf(false) }
    var selectedValue by remember { mutableFloatStateOf(0F) }

    // animate a return to zero when released
    val bounceValue: Float by animateFloatAsState(
        targetValue = selectedValue,
        animationSpec = if (userInteracting) snap() else spring(stiffness = Spring.StiffnessLow)
    )

    // emit the current value every 50ms when held by the user
    LaunchedEffect(userInteracting) {
        while (userInteracting) {
            delay(50)
            onValueChange(selectedValue)
        }
    }

    Slider(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        value = bounceValue,
        valueRange = -1F..1F,
        onValueChange = { newValue ->
            userInteracting = true
            selectedValue = newValue
        },
        onValueChangeFinished = {
            userInteracting = false
            selectedValue = 0F
        },
        colors = SliderDefaults.colors().copy(
            activeTrackColor = SliderDefaults.colors().inactiveTrackColor,
            disabledActiveTrackColor = SliderDefaults.colors().disabledInactiveTrackColor
        )
    )
}

@Composable
@Preview
internal fun JoysliderPreview(){
    Column(Modifier.padding().background(Color.White)) {
        var value by remember { mutableFloatStateOf(0F) }
        Row {
            Text(
                style = TextStyle.Default.copy(),
                text="Value: " + DecimalFormat("#.##").format(value))
        }
        Joyslider (
            onValueChange = { newValue -> value += newValue},
            contentDescription = "Slider"
            )
    }
}
