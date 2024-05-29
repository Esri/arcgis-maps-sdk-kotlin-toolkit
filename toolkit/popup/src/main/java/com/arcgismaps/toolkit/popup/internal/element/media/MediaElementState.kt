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

package com.arcgismaps.toolkit.popup.internal.element.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupMedia
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.mapping.popup.PopupMediaValue
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState

/**
 * Represents the state of an [MediaPopupElement]
 */
@Immutable
internal class MediaElementState(
    val description: String,
    val title: String,
    val media: List<PopupMediaState>,
    override val id : Int = createId(),
) : PopupElementState() {

    constructor(mediaPopupElement: MediaPopupElement): this(
        description = mediaPopupElement.description,
        title = mediaPopupElement.title,
        media = mediaPopupElement.media
            .filter { it.type is PopupMediaType.Image }
            .map { PopupMediaState(it) }
    )

    companion object {
        fun Saver(
            element: MediaPopupElement
        ): Saver<MediaElementState, Any> = Saver(
            save = { null },
            restore = {
                MediaElementState(
                    element
                )
            }
        )

    }
}

@Composable
internal fun rememberMediaElementState(
    element: MediaPopupElement,
    popup: Popup
): MediaElementState {
    return rememberSaveable(
        inputs = arrayOf(popup, element),
        saver = MediaElementState.Saver(element)
    ) {
        MediaElementState(
            element
        )
    }
}

/**
 * Represents the state of a [PopupMedia value].
 */
internal class PopupMediaState(
    val title: String,
    val caption: String,
    val refreshInterval: Long,
    val media: PopupMediaValue,
    val type: PopupMediaType
) {
    constructor(media: PopupMedia) : this(
        title = media.title,
        caption = media.caption,
        refreshInterval = media.imageRefreshInterval,
        media = media.value!!,
        type = media.type
    )
}

@Composable
internal fun PopupMediaType.getIcon(): ImageVector = when (this) {
    PopupMediaType.Image -> Icons.Outlined.Image
    else -> Icons.Outlined.BarChart
}
