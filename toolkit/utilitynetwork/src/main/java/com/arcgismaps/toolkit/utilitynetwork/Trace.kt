/*
 *  Copyright 2024 Esri
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
 */

package com.arcgismaps.toolkit.utilitynetwork

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.utilitynetworks.UtilityNetwork

/**
 * A composable UI component to set up and run a [com.arcgismaps.utilitynetworks.UtilityNetwork.trace]
 * on a [com.arcgismaps.toolkit.geoviewcompose.MapView].
 *
 * @param utilityNetwork a [UtilityNetwork]
 * @since 200.6.0
 */
@Composable
public fun Trace(utilityNetwork: UtilityNetwork, @Suppress("unused_parameter") modifier: Modifier = Modifier) {
    Text(text = utilityNetwork.name)
}
