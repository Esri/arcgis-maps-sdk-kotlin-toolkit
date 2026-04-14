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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.SpeechRecognizer
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun VoiceToForm(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speechRecognizer = remember {
        SpeechRecognizer()
    }
    val scope = rememberCoroutineScope()
    var isProcessing by rememberSaveable {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss voice input",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Listening...", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.GraphicEq, contentDescription = "Voice recognition in progress")
            }
            IconButton(
                onClick = {
                    scope.launch {
                        speechRecognizer.stopVoiceRecognition()
                        isProcessing = true
                    }
                }
            ) {
                Icon(Icons.Default.Done, contentDescription = "Stop voice recognition")
            }
        }
    }
    if (isProcessing) {
        VoiceResultDialog(
            text = speechRecognizer.response.collectAsState().value,
            onDismiss = {
                isProcessing = false
                onDismiss()
            }
        )
    }
    LaunchedEffect(speechRecognizer) {
        speechRecognizer.initialize().onSuccess {
            speechRecognizer.startVoiceRecognition()
            Log.e("TAG", "VoiceToForm: ready")
        }.onFailure {
            Log.e("TAG", "VoiceToForm: failed to initialize speech recognizer")
        }
    }
    LaunchedEffect(speechRecognizer) {
        speechRecognizer.response.collect {
            Log.e("TAG", "VoiceToForm: $it")
        }
    }
}

@Composable
private fun VoiceResultDialog(
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {

        },
        confirmButton = {
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Dismiss voice input"
                )
            }
        },
        dismissButton = {
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss voice input",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        icon = {
            Icon(Icons.Default.GraphicEq, contentDescription = "Voice recognition result")
        },
        text = {
            Text(text)
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview
@Composable
private fun VoiceToFormPreview() {
    MaterialTheme {
        VoiceToForm({})
    }
}
