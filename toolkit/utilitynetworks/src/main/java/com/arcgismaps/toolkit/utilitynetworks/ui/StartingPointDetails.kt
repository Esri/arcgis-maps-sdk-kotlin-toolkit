/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.toolkit.utilitynetworks.ui.material3.Slider
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint


@Composable
internal fun StartingPointDetails(startingPoint: StartingPoint?,
                                  onFractionChanged: (StartingPoint, Float) -> Unit,
                                  onBackPressed: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top) {
            Row(
                modifier = Modifier.clickable { onBackPressed() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            Image(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "back"
            )
            ReadOnlyTextField(
                text = stringResource(id = R.string.starting_points))
            }
        }

        startingPoint?.let { point ->
            val geometry = point.feature.geometry

            if (geometry != null && geometry is Polyline ) {
                FractionAlong(point, onFractionChanged)
            }
        }

    }
}

@Composable
private fun FractionAlong(startingPoint: StartingPoint, onFractionChanged: (StartingPoint, Float) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(startingPoint.utilityElement.fractionAlongEdge.toFloat()) }
    Slider(
        value = sliderValue,
        onValueChange = { newValue ->
            sliderValue = newValue
            onFractionChanged(startingPoint, newValue)
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SliderPreview() {
    Slider(
        value = 0.5f,
        onValueChange = {

        }

    )
}

@Preview(showBackground = true)
@Composable
internal fun StartingPointDetailsPreview() {
    StartingPointDetails(null, onFractionChanged = { _, _ -> }) {

    }

}
