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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.featureforms.R
import kotlinx.coroutines.launch

/**
 * Displays an in-app audio recorder. When audio is captured, the [onAudioCaptured] callback is
 * invoked with the URI of the captured audio. In case of a dismissal or if no audio is captured,
 * the [onDismissRequest] callback is invoked.
 *
 * @param viewModel The [AudioCaptureViewModel] that manages the state and logic for audio recording.
 * @param onDismissRequest A request to dismiss the audio recorder.
 * @param modifier The [Modifier] to apply.
 */
@Composable
internal fun AudioCapture(
    viewModel: AudioCaptureViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onDismiss by rememberUpdatedState(onDismissRequest)
    var isRecording by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        viewModel.setOnAudioCaptureCompleteAction(onDismiss)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setOnAudioCaptureCompleteAction(null)
        }
    }

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.record_audio),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                //modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    error != null -> Text(text = error!!)
                    isRecording -> TimerTextProvider(viewModel.maxDuration) { viewModel.elapsedSeconds }
                    else -> {
                        Text(text = stringResource(R.string.ready_to_record_audio))
                    }
                }
            }
            Row() {
                TextButton(
                    onClick = {
                        viewModel.cancelRecording()
                        onDismissRequest()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            scope.launch {
                                viewModel.startRecording()
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (isRecording) {
                            stringResource(R.string.stop_recording)
                        } else {
                            stringResource(R.string.start_recording)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerTextProvider(maxDuration: Long?, textProvider: () -> Long) {
    val locale = LocalLocale.current.platformLocale
    val maxDurationText = remember(maxDuration) {
        maxDuration
            ?.takeIf { it > 0 }
            ?.let {
                val maxMins = it / 60
                val maxSecs = it % 60
                String.format(locale, "%02d:%02d", maxMins, maxSecs)
            }
    }
    val elapsedSeconds by rememberUpdatedState(textProvider())
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val text = String.format(locale, "%02d:%02d", mins, secs)
    // Only this specific Text node updates
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = "Recording",
            tint = Color.Red
        )
        Text(text = maxDurationText?.let { "$text / $it" } ?: text)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AudioCapturePreview() {
    AudioCapture(
        viewModel = viewModel<AudioCaptureViewModel>(
            factory = AudioCaptureViewModel.Factory(
                maxDuration = 10_000L,
                onAudioCaptured = { Result.success(Unit) }
            )
        ),
        maxDuration = 10_000L,
        onDismissRequest = {},
    )
}
