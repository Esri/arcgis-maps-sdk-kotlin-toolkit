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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityElement

/**
 * A composable that represents a percent along control of a [UtilityAssociation]. This can
 * represent the [UtilityAssociation.fractionAlongEdge] property if one of the element is a non-spatial
 * edge or the [UtilityElement.fractionAlongEdge] when the element represents a spatial edge.
 *
 * This control displays a slider that allows the user to select a value between 0 and 1, which is then
 * multiplied by 100 to display the percentage value.
 *
 * @param initialFraction The initial value of the percent along control.
 * @param enabled A boolean indicating whether the control is enabled or not.
 * @param onValueChanged A callback that is called when the value of the control changes.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun PercentAlongControl(
    initialFraction: Double,
    enabled: Boolean,
    onValueChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember {
        mutableDoubleStateOf(initialFraction)
    }
    val percent = (value * 100).toInt()
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PropertyRow(
                title = stringResource(R.string.percent_along),
                value = stringResource(R.string.percent_along_edge, percent),
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    value = (it * 100).toInt() / 100.0
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.contentDescription = "percent along slider"
                    },
                onValueChangeFinished = {
                    onValueChanged(value)
                }
            )
        }
    }
}

@Preview
@Composable
private fun PercentAlongControlPreview() {
    PercentAlongControl(
        initialFraction = 0.5,
        onValueChanged = {},
        enabled = true,
        modifier = Modifier.fillMaxWidth()
    )
}
