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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * Displays an in-app audio recorder.
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
    val onDismiss by rememberUpdatedState(onDismissRequest)
    val state = viewModel.state

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(state) {
        if (state is AudioCaptureState.Stopped) {
            onDismiss()
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
                imageVector = if (state is AudioCaptureState.Error) {
                    Icons.Outlined.Error
                } else {
                    Icons.Outlined.GraphicEq
                },
                contentDescription = null,
                tint = if (state is AudioCaptureState.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    LocalContentColor.current
                },
            )
            when (state) {

                is AudioCaptureState.Ready -> {
                    ReadyToRecord(
                        onStartRecording = viewModel::startRecording,
                        onCancel = onDismiss
                    )
                }

                is AudioCaptureState.Error -> {
                    Error(onDismissRequest = onDismiss, message = state.message)
                }

                is AudioCaptureState.Recording -> {
                    Recording(
                        onStopRecording = viewModel::stopRecording,
                        onCancel = viewModel::cancelRecording,
                        elapsedSecondsProvider = { viewModel.elapsedSeconds },
                        maxDuration = viewModel.maxDuration,
                    )
                }

                AudioCaptureState.NotReady, AudioCaptureState.Saving -> {
                    CircularProgressIndicator()
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ReadyToRecord(
    onStartRecording: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.ready_to_record_audio),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onStartRecording,
        ) {
            Icon(
                imageVector = Icons.Filled.FiberManualRecord,
                contentDescription = null,
                tint = Color.Red
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.start_recording))
        }
    }
}

@Composable
private fun Recording(
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    elapsedSecondsProvider: () -> Long,
    maxDuration: Long?,
) {
    TimerTextProvider(maxDuration, elapsedSecondsProvider)
    Spacer(modifier = Modifier.height(12.dp))
    Row {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onStopRecording,
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                tint = Color.Red
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.stop_recording))
        }
    }
}

@Composable
private fun Error(
    onDismissRequest: () -> Unit,
    message: String
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
    TextButton(onClick = onDismissRequest) {
        Text(text = stringResource(R.string.ok))
    }
}

@Composable
private fun TimerTextProvider(maxDuration: Long?, textProvider: () -> Long) {
    val locale = LocalLocale.current.platformLocale
    val maxDurationText = remember(maxDuration, locale) {
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
        Spacer(modifier = Modifier.width(4.dp))
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
        onDismissRequest = {},
    )
}
