/*
 * Copyright 2025 Esri
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun DeleteAttachmentDialog(
    attachmentName: String,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.delete_attachment_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.delete_attachment_confirmation),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = attachmentName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        confirmButton = {
            TextButton (
                onClick = onDelete
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        },
        dismissButton = {
            TextButton (
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Preview
@Composable
private fun DeleteAttachmentDialogPreview() {
    DeleteAttachmentDialog(
        attachmentName = "test.txt",
        onDelete = {},
        onDismissRequest = {}
    )
}
