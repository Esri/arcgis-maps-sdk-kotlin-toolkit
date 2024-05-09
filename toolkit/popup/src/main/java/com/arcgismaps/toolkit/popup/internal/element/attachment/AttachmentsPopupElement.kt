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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.popup.PopupAttachmentType
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AttachmentsPopupElement(
    state: AttachmentsElementState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AttachmentsPopupElement(
        title = state.title,
        description = state.description,
        stateId = state.id,
        attachments = state.attachments,
        modifier = modifier
    )
}

@Composable
internal fun AttachmentsPopupElement(
    description: String,
    title: String,
    stateId: Int,
    attachments: List<PopupAttachmentState>,
    modifier: Modifier = Modifier,
    colors: AttachmentsElementColors = AttachmentsElementDefaults.colors()
) {
    Card(
        modifier = modifier,
        shape = AttachmentsElementDefaults.containerShape,
        border = BorderStroke(AttachmentsElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            val listState = rememberLazyListState()
            Row {
                Header(
                    title = title,
                    description = description
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
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

@Composable
private fun Header(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun AttachmentsPopupElementPreview() {
    AttachmentsPopupElement(
        title = "Attachments",
        description = "Add attachments",
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
