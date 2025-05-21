/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun FractionAlongEdgeControl(
    fraction : Float,
    enabled : Boolean,
    onValueChanged : (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember {
        mutableFloatStateOf(fraction)
    }
    val percent = (value * 100).toInt()
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PropertyRow(
                title = stringResource(R.string.fraction_along_edge),
                value = "$percent %",
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = value,
                onValueChange = {
                    value = it
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                onValueChangeFinished = {
                    onValueChanged(value)
                }
            )
        }
    }
}

@Preview
@Composable
private fun FractionAlongEdgeControlPreview() {
    FractionAlongEdgeControl(
        fraction = 0.5f,
        onValueChanged = {},
        enabled = true,
        modifier = Modifier.fillMaxWidth()
    )
}
