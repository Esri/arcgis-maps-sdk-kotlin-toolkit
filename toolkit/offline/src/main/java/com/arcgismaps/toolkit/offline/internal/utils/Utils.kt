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

package com.arcgismaps.toolkit.offline.internal.utils

import android.text.Html
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.R
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreasState
import java.io.File

/**
 * Calculates the total size of a [path] recursively.
 *
 * @since 200.8.0
 */
internal fun getDirectorySize(path: String): Int {
    val file = File(path)
    if (!file.exists()) return 0
    if (file.isFile) return file.length().toInt()
    return file.walkTopDown().filter { it.isFile }.map { it.length() }.sum().toInt()
}

/**
 * Formats a given [size] in bytes into readable string.
 * (e.g., `123 B`, `1.2 KB`, `5.6 MB`, `2.3 GB`)
 *
 * @since 200.8.0
 */
internal fun formatSize(size: Int): String {
    return when {
        size < 1000 -> "$size B"
        size < 1000 * 1000 -> "%.1f KB".format(size / 1000.0)
        size < 1000 * 1000 * 1000 -> "%.1f MB".format(size / (1000.0 * 1000.0))
        else -> "%.1f GB".format(size / (1000.0 * 1000.0 * 1000.0))
    }
}


/**
 * Converts an [html] string to a plain text string.
 *
 * @since 200.8.0
 */
internal fun htmlToPlainText(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
}

/**
 * Returns a unique default map area title: "Area 1","Area 2"...
 *
 * @since 200.8.0
 */
@Composable
internal fun getDefaultMapAreaTitle(onDemandMapAreaStates: List<OnDemandMapAreasState>): String {
    for (i in 1..Int.MAX_VALUE) {
        val title = stringResource(R.string.next_on_demand_area_title, i.toString())
        if (onDemandMapAreaStates.none { it.title == title }) {
            return title
        }
    }
    return ""
}

/**
 * Returns true if the [mapAreaTitle] is unique title among all [onDemandMapAreaStates].
 *
 * @since 200.8.0
 */
internal fun isValidMapAreaTitle(
    mapAreaTitle: String,
    onDemandMapAreaStates: List<OnDemandMapAreasState>
): Boolean {
    if (mapAreaTitle.isBlank())
        return false
    if (onDemandMapAreaStates.any { it.title == mapAreaTitle })
        return false
    return true
}

/**
 * Returns an [Envelope] which is equal to 80% of the ratio of [mapViewSize]
 * at the current viewpoint, to be treated as the area of interest.
 *
 * @since 200.8.0
 */
internal fun calculateEnvelope(mapViewSize: IntSize, mapViewProxy: MapViewProxy): Envelope? {
    val inh = mapViewSize.width * 0.1
    val inv = mapViewSize.height * 0.1
    val minScreen = ScreenCoordinate(x = inh, y = inv)
    val maxScreen = ScreenCoordinate(x = mapViewSize.width - inh, y = mapViewSize.height - inv)
    val minResult = mapViewProxy.screenToLocationOrNull(minScreen)
    val maxResult = mapViewProxy.screenToLocationOrNull(maxScreen)
    return if (minResult != null && maxResult != null) {
        Envelope(min = minResult, max = maxResult)
    } else null
}
