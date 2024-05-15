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

package com.arcgismaps.toolkit.popup.internal.element.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.popup.PopupAttachmentType
import com.arcgismaps.toolkit.popup.internal.ui.ExpandableCard
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AttachmentsPopupElement(
    state: AttachmentsElementState
) {
    AttachmentsPopupElement(
        title = state.title,
        description = state.description,
        stateId = state.id,
        attachments = state.attachments
    )
}

@Composable
internal fun AttachmentsPopupElement(
    description: String,
    title: String,
    @Suppress("UNUSED_PARAMETER") stateId: Int,
    attachments: List<PopupAttachmentState>
) {
    ExpandableCard(
        title = title,
        description = description,
        elementCount = attachments.size
    ) {
        Column(
            modifier = Modifier.padding(AttachmentsElementDefaults.shapes().galleryPadding)
        ) {
            val listState = rememberLazyListState()
            AttachmentGallery(listState, attachments)
        }
    }
}

@Composable
private fun AttachmentGallery(state: LazyListState, attachments: List<PopupAttachmentState>) {
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(attachments, key = { it.name + it.type + it.size }) {
            AttachmentTile(it)
        }
    }
}


@Preview
@Composable
private fun AttachmentsPopupElementPreview() {
    AttachmentsPopupElement(
        title = "Attachments",
        description = "description of attachments",
        stateId = 1,
        attachments = listOf(
            PopupAttachmentState(
                "Photo 1.jpg",
                2024,
                PopupAttachmentType.Image,
                MutableStateFlow(LoadStatus.Loaded),
                { Result.success(Unit) },
                { Result.success(null) }
            )
        )
    )
}
