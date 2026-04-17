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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.FeatureFormGenerativeModel
import com.arcgismaps.toolkit.featureforms.internal.components.mlkit.SpeechRecognizer
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun VoiceToForm(
    model: FeatureFormGenerativeModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speechRecognizer = remember {
        SpeechRecognizer()
    }
    val scope = rememberCoroutineScope()
    var isProcessing by remember {
        mutableStateOf(false)
    }
    var showDialog by remember {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDialog.not()) {
                    Text("Listening...", style = MaterialTheme.typography.bodyLarge)
                    AnimatedGraphicEq(
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text("Processing..", style = MaterialTheme.typography.bodyLarge)
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        speechRecognizer.stopVoiceRecognition()
                        showDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Done, contentDescription = "Stop voice recognition")
            }
        }
    }
    if (showDialog) {
        ProcessVoiceDialog(
            text = speechRecognizer.response.collectAsState().value,
            isProcessing = isProcessing,
            onConfirm = {
                isProcessing = true
                scope.launch {
                    val userPrompt = """
                    User's spoken input converted to text:
                    ${speechRecognizer.response.value}
                    """.trimIndent()
                    model.getResponse(userPrompt)
                    onDismiss()
                }
            },
            onDiscard = onDismiss
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
}

@Composable
private fun AnimatedGraphicEq(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val transition = rememberInfiniteTransition(label = "graphicEq")

    // Three bars with phase offsets.
    val barA by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barA"
    )
    val barB by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing, delayMillis = 120),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barB"
    )
    val barC by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing, delayMillis = 240),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barC"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        EqBar(fraction = barA, color = barColor)
        EqBar(fraction = barB, color = barColor)
        EqBar(fraction = barC, color = barColor)
    }
}

@Composable
private fun EqBar(
    fraction: Float,
    color: Color,
    maxHeight: Dp = 20.dp,
    width: Dp = 4.dp,
) {
    val clamped = fraction.coerceIn(0.1f, 1f)
    Box(
        modifier = Modifier
            .width(width)
            .height(maxHeight * clamped)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
