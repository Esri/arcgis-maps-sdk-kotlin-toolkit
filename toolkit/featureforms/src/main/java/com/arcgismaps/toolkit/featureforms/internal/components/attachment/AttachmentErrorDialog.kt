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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun AttachmentErrorDialog(
    errorTitle: String,
    errorMessage: String,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = errorTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton (
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(R.string.ok))
            }
        }
    )
}

@Preview
@Composable
private fun AttachmentErrorDialogPreview() {
    AttachmentErrorDialog(
        errorTitle = "Error",
        errorMessage = "An error occurred while processing the attachment.",
        onDismissRequest = {}
    )
}
