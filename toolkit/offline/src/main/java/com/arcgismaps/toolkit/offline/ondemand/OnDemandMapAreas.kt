/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offline.ondemand

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a list of on-demand map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun OnDemandMapAreas(
    onDemandMapAreasStates: List<OnDemandMapAreasState>,
    modifier: Modifier
) {
    Column {
        onDemandMapAreasStates.forEach { state ->
            Text(state.title)
        }
    }
}
