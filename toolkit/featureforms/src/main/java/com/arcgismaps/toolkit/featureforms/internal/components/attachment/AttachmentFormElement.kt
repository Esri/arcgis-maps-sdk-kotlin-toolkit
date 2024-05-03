/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AttachmentFormElement(
    state: AttachmentElementState,
    modifier: Modifier = Modifier
) {
    AttachmentFormElement(
        label = state.label,
        description = state.description,
        editable = true,
        attachments = state.attachments,
        modifier = modifier
    )
}

@Composable
internal fun AttachmentFormElement(
    label: String,
    description: String,
    editable: Boolean,
    attachments: List<FormAttachmentState>,
    modifier: Modifier = Modifier,
    colors: AttachmentElementColors = AttachmentElementDefaults.colors()
) {
    Card(
        modifier = modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Header(
                title = label,
                description = description,
                editable = editable
            )
            Spacer(modifier = Modifier.height(20.dp))
            Carousel(attachments)
        }
    }
}

@Composable
private fun Carousel(attachments: List<FormAttachmentState>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(attachments) {
            AttachmentTile(it)
        }
    }
}

@Composable
private fun Header(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    editable: Boolean = true
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
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun AttachmentFormElementPreview() {
    AttachmentFormElement(
        label = "Attachments",
        description = "Add attachments",
        editable = true,
        attachments = listOf(
            FormAttachmentState(
                "Photo 1.jpg",
                2024,
                MutableStateFlow(LoadStatus.Loaded),
                { Result.success(Unit) },
                { Result.success(null) },
                scope = rememberCoroutineScope()
            )
        )
    )
}
