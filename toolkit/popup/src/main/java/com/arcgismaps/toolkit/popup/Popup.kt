/*
 *
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
 *
 */

package com.arcgismaps.toolkit.popup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.mapping.popup.Popup

@Immutable
private data class PopupState(@Stable val popup: Popup)

/**
 * A composable Popup toolkit component that enables users to see Popup content in a
 * layer that have been configured externally.
 *
 * Popups may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)).
 *
 * Note : Even though the [Popup] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param popup The [Popup] configuration.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * Popup.
 *
 * @since 200.5.0
 */
@Suppress("unused_parameter", "unused")
@Composable
public fun Popup(popup: Popup, modifier: Modifier = Modifier) {
    Text(text = popup.title)
}

@Suppress("unused_parameter")
@Composable
private fun Popup(popupState: PopupState, modifier: Modifier = Modifier) {
    Popup(popupState.popup.title)
}

@Suppress("unused_parameter")
@Composable
private fun Popup(title: String, modifier: Modifier = Modifier) {
    Text(text = title)
}
@Preview
@Composable
internal fun PopupPreview() {
    Popup(title = "Hello")
}
