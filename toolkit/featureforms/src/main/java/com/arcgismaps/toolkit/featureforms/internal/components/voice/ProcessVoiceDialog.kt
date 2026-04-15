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

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.FeatureFormPromptResponse
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.PromptModel
import kotlinx.coroutines.launch

@Composable
internal fun ProcessVoiceDialog(
    prefix: String,
    text: String,
    onDismiss: () -> Unit,
    onProcessResult: (FeatureFormPromptResponse) -> Unit
) {
    val scope = rememberCoroutineScope()
    val userPrompt = remember(text) {
        """
        User's spoken input converted to text:
        $text
        """.trimIndent()
    }
    val promptModel = remember {
        PromptModel(prefix)
    }
    AlertDialog(
        onDismissRequest = {

        },
        confirmButton = {
            IconButton(
                onClick = {
                    scope.launch {
                        promptModel.getResponse(userPrompt).onSuccess {
                            val response =
                                FeatureFormPromptResponse.fromJsonOrNull(it.extractJsonObject())
                            if (response != null) {
                                onProcessResult(response)
                            }
                            Log.e("TAG", "VoiceResultDialog: response: $it")
                            Log.e("TAG", "VoiceResultDialog: $response")
                        }.onFailure {
                            Log.e("TAG", "VoiceResultDialog: failed to get response: ${it.message}")
                        }
                    }
                }
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
    LaunchedEffect(promptModel) {
        promptModel.initialize().onSuccess {
            Log.e("TAG", "VoiceResultDialog: Prompt model is ready")
        }.onFailure {
            Log.e("TAG", "VoiceResultDialog: Failed to initialize prompt model")
        }
    }
}

private fun String.extractJsonObject(): String {
    val trimmed = trim()
    return if (trimmed.startsWith("```")) {
        trimmed.substringAfter('\n', trimmed)
            .substringBeforeLast("```")
            .trim()
    } else {
        trimmed
    }
}
