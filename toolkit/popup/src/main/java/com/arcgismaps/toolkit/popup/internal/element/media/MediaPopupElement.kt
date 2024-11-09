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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.ViewableFile
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.theme.LocalExpandableCardColorScheme
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.theme.LocalExpandableCardTypography

@Composable
internal fun MediaPopupElement(
    state: MediaElementState,
    onClickedMedia: (ViewableFile) -> Unit
) {
    MediaPopupElement(
        title = state.title,
        description = state.description,
        stateId = state.id,
        media = state.media,
        onClickedMedia = onClickedMedia
    )
}

@Composable
private fun MediaPopupElement(
    description: String,
    title: String,
    @Suppress("UNUSED_PARAMETER") stateId: Int,
    media: List<PopupMediaState>,
    onClickedMedia: (ViewableFile) -> Unit
) {
    ExpandableCard(
        title = title,
        description =  {
            Text(
                text = description,
                color = LocalExpandableCardColorScheme.current.headerTextColor,
                style = LocalExpandableCardTypography.current.descriptionStyle,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(MediaElementDefaults.shapes().galleryPadding)
        ) {
            val listState = rememberLazyListState()
            MediaGallery(listState, media, onClickedMedia)
        }
    }
}

@Composable
private fun MediaGallery(state: LazyListState, media: List<PopupMediaState>, onClicked: (ViewableFile) -> Unit) {
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(media, key = { it.title + it.type + it.caption }) {
            MediaTile(
                state = it,
                onClicked = onClicked
            )
        }
    }
}
