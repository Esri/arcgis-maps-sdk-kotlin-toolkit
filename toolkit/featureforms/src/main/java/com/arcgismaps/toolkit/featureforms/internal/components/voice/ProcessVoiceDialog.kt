/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.voice

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun ProcessVoiceDialog(
    text: String,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* no-op, force user to choose an action */ },
        confirmButton = {
            if (!isProcessing) {
                IconButton(onClick = onConfirm) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Dismiss voice input"
                    )
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                IconButton(onClick = onDiscard) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Dismiss voice input",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        icon = {
            Image(
                painterResource(R.drawable.gemini),
                contentDescription = "Voice recognition logo",
                modifier = Modifier.size(32.dp)
            )
        },
        text = {
            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 5.dp
                    )
                }
            } else {
                Text(text)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Preview
@Composable
internal fun ProcessVoiceDialogPreview() {
    ProcessVoiceDialog(
        text = "Recognized voice input will be displayed here.",
        isProcessing = true,
        onConfirm = { },
        onDiscard = { }
    )
}

