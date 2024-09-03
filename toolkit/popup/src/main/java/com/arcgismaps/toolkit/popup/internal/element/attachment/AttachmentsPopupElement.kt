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
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.ViewableFile
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard

@Composable
internal fun AttachmentsPopupElement(
    state: AttachmentsElementState,
    onSelectedAttachment: (ViewableFile) -> Unit = {}
) {
    AttachmentsPopupElement(
        title = state.title,
        description = state.description,
        stateId = state.id,
        attachments = state.attachments,
        onSelectedAttachment = onSelectedAttachment
    )
}

@Composable
private fun AttachmentsPopupElement(
    description: String,
    title: String,
    @Suppress("UNUSED_PARAMETER") stateId: Int,
    attachments: List<PopupAttachmentState>,
    onSelectedAttachment: (ViewableFile) -> Unit = {}
) {
    ExpandableCard(
        title = title,
        description = description
    ) {
        Column(
            modifier = Modifier.padding(AttachmentsElementDefaults.shapes().galleryPadding)
        ) {
            val listState = rememberLazyListState()
            AttachmentGallery(listState, attachments, onSelectedAttachment)
        }
    }
}

@Composable
private fun AttachmentGallery(
    state: LazyListState,
    attachments: List<PopupAttachmentState>,
    onSelectedAttachment: (ViewableFile) -> Unit = {}
) {
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(attachments, key = { it.name + it.popupAttachmentType + it.size }) {
            AttachmentTile(it, onSelectedAttachment)
        }
    }
}
