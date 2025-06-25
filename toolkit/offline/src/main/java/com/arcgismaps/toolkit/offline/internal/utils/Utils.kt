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
import androidx.compose.ui.unit.IntSize
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreasState
import java.io.File

internal fun getDirectorySize(path: String): Int {
    val file = File(path)
    if (!file.exists()) return 0
    if (file.isFile) return file.length().toInt()
    return file.walkTopDown().filter { it.isFile }.map { it.length() }.sum().toInt()
}

internal fun formatSize(size: Int): String {
    return when {
        size < 1000 -> "$size B"
        size < 1000 * 1000 -> "%.1f KB".format(size / 1000.0)
        size < 1000 * 1000 * 1000 -> "%.1f MB".format(size / (1000.0 * 1000.0))
        else -> "%.1f GB".format(size / (1000.0 * 1000.0 * 1000.0))
    }
}

internal fun htmlToPlainText(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
}

internal fun getDefaultMapAreaTitle(onDemandMapAreaStates: List<OnDemandMapAreasState>): String {
    for (i in 1..Int.MAX_VALUE) {
        val title = "Area $i"
        if (onDemandMapAreaStates.none { it.title == title }) {
            return title
        }
    }
    return ""
}

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

internal fun calculateEnvelope(fullSize: IntSize, mapViewProxy: MapViewProxy): Envelope? {
    val inh = fullSize.width * 0.2 / 2
    val inv = fullSize.height * 0.2 / 2
    val minScreen = ScreenCoordinate(x = inh, y = inv)
    val maxScreen = ScreenCoordinate(x = fullSize.width - inh, y = fullSize.height - inv)
    val minResult = mapViewProxy.screenToLocationOrNull(minScreen)
    val maxResult = mapViewProxy.screenToLocationOrNull(maxScreen)
    return if (minResult != null && maxResult != null) {
        Envelope(min = minResult, max = maxResult)
    } else null
}
