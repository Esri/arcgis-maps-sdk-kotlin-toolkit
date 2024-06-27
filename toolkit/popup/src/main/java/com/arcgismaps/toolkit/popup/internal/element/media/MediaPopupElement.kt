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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.popup.PopupMediaType
import com.arcgismaps.toolkit.popup.internal.ui.ExpandableCard

@Composable
internal fun MediaPopupElement(
    state: MediaElementState
) {
    MediaPopupElement(
        title = state.title,
        description = state.description,
        stateId = state.id,
        media = state.media
    )
}

@Composable
private fun MediaPopupElement(
    description: String,
    title: String,
    @Suppress("UNUSED_PARAMETER") stateId: Int,
    media: List<PopupMediaState>
) {
    ExpandableCard(
        title = title,
        description = description
    ) {
        Column(
            modifier = Modifier.padding(MediaElementDefaults.shapes().galleryPadding)
        ) {
            val listState = rememberLazyListState()
            MediaGallery(listState, media)
        }
    }
}

@Composable
private fun MediaGallery(state: LazyListState, media: List<PopupMediaState>) {
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(media, key = { it.title + it.type + it.caption }) {
            MediaTile(it)
        }
    }
}

@Preview
@Composable
private fun MediaPopupElementPreview() {
    MediaPopupElement(
        title = "Media",
        description = "description of Media",
        stateId = 1,
        media = listOf(
            PopupMediaState(
                title = "Photo 1.jpg",
                caption = "caption",
                refreshInterval = 1234L,
                linkUrl = "",
                sourceUrl = "https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png",
                type = PopupMediaType.Image,
                scope = rememberCoroutineScope(),
                chartFolder = ""
            )
        )
    )
}
