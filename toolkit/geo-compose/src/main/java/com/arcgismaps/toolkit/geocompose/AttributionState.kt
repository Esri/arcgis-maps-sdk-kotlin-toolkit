/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.view.AttributionBarLayoutChangeEvent
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.launch

/**
 * State holder for attribution bar related properties/events on the [com.arcgismaps.toolkit.geocompose.MapView].
 *
 * @property isAttributionBarVisible true if the attribution bar is visible in the GeoView, false otherwise
 * @property onAttributionTextChanged called when the attribution text for the data that is currently
 * displayed in the GeoView changes
 * @property onAttributionBarLayoutChanged called when the attribution bar's position or size changes
 * due to expanding, collapsing, or inset changes.
 * @since 200.4.0
 */
@Stable
public data class AttributionState(
    val isAttributionBarVisible: Boolean = true,
    val onAttributionTextChanged: ((String) -> Unit)? = null,
    val onAttributionBarLayoutChanged: ((AttributionBarLayoutChangeEvent) -> Unit)? = null
)

/**
 * Sets up the attribution bar's property and events.
 *
 * @since 200.4.0
 */
@Composable
internal fun AttributionStateHandler(geoView: GeoView, attributionState: AttributionState) {
    LaunchedEffect(attributionState) {
        // isAttributionBarVisible does not take effect if applied in the AndroidView update callback
        geoView.isAttributionBarVisible = attributionState.isAttributionBarVisible
        launch {
            geoView.attributionText.collect {
                attributionState.onAttributionTextChanged?.invoke(it)
            }
        }
        launch {
            geoView.onAttributionBarLayoutChanged.collect { attributionBarLayoutChangedEvent ->
                attributionState.onAttributionBarLayoutChanged?.invoke(
                    attributionBarLayoutChangedEvent
                )
            }
        }
    }
}

