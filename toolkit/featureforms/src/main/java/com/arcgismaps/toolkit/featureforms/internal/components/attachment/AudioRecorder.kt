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

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.FileUriReference
import com.arcgismaps.toolkit.featureforms.internal.utils.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Displays an in-app audio recorder. When audio is captured, the [onAudioCaptured] callback is
 * invoked with the URI of the captured audio. In case of a dismissal or if no audio is captured,
 * the [onDismissRequest] callback is invoked.
 *
 * @param maxDuration The maximum duration limit for the captured audio.
 * @param onDismissRequest A request to dismiss the audio recorder.
 * @param onAudioCaptured A callback to invoke when audio is captured.
 */
@Composable
internal fun AudioCapture(
    maxDuration: Long?,
    onDismissRequest: () -> Unit,
    onAudioCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnAudioCaptured by rememberUpdatedState(onAudioCaptured)
    val audioRecorder = remember(context, scope, maxDuration) {
        AudioRecorder(
            context = context,
            coroutineScope = scope,
            maxDuration = maxDuration,
            onMaxDurationReached = { currentOnAudioCaptured(it) }
        )
    }
    var isRecording by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember(audioRecorder) { mutableLongStateOf(0L) }

    DisposableEffect(audioRecorder) {
        onDispose {
            audioRecorder.cancel()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0L
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            audioRecorder.cancel()
            onDismissRequest()
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null
            )
        },
        title = { Text(text = stringResource(R.string.record_audio)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    error != null -> Text(text = error!!)
                    isRecording -> TimerTextProvider(maxDuration) { elapsedSeconds }
                    else -> {
                        Text(text = stringResource(R.string.ready_to_record_audio))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isRecording) {
                        audioRecorder.stop().fold(
                            onSuccess = { uri -> onAudioCaptured(uri) },
                            onFailure = { ex ->
                                isRecording = false
                                error = ex.message
                            }
                        )
                    } else {
                        scope.launch {
                            audioRecorder.start().fold(
                                onSuccess = {
                                    isRecording = true
                                    error = null
                                },
                                onFailure = { ex -> error = ex.message }
                            )
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
        },
        dismissButton = {
            TextButton(
                onClick = {
                    audioRecorder.cancel()
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    )
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

private class AudioRecorder(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val maxDuration: Long?,
    private val onMaxDurationReached: (File) -> Unit
) {
    private var recorder: MediaRecorder? = null
    private var fileUriReference: FileUriReference? = null
    private var isRecording = false

    suspend fun start(): Result<Unit> = runCatchingCancellable {
        withContext(Dispatchers.IO) {
            val timeStamp = Instant.now().toEpochMilli()
            fileUriReference = AttachmentsFileProvider.createTempFileWithUri(
                "AUDIO_$timeStamp",
                ".m4a",
                context
            )
            recorder = context.createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(fileUriReference!!.file.absolutePath)
                maxDuration?.let { duration ->
                    if (duration > 0) {
                        // The max duration is set to 1 second less than the specified maxDuration
                        // to ensure that the recording stops just before reaching the limit.
                        val dur = duration.minus(1).toMillisecondsIntClamped()
                        setMaxDuration(dur)
                    }
                }
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        coroutineScope.launch {
                            this@AudioRecorder.stop().onSuccess(onMaxDurationReached)
                        }
                    }
                }
                prepare()
                start()
            }
            isRecording = true
        }
    }

    fun stop(): Result<File> = runCatching {
        check(isRecording) { "Audio recording has not started." }
        val file = checkNotNull(fileUriReference?.file) {
            "Audio recording URI is unavailable."
        }
        recorder?.stop()
        releaseRecorder()
        isRecording = false
        file
    }.onFailure {
        cancel()
    }

    fun cancel() {
        if (isRecording) {
            runCatching { recorder?.stop() }
        }
        releaseRecorder()
        isRecording = false
        fileUriReference?.file?.delete()
    }

    private fun releaseRecorder() {
        recorder?.reset()
        recorder?.release()
        recorder = null
    }
}

@Suppress("DEPRECATION")
private fun Context.createMediaRecorder(): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(this)
    } else {
        MediaRecorder()
    }

private fun Long.toMillisecondsIntClamped(): Int = when {
    this > Int.MAX_VALUE / 1_000L -> Int.MAX_VALUE
    this < Int.MIN_VALUE / 1_000L -> Int.MIN_VALUE
    else -> (this * 1_000L).toInt()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AudioCapturePreview() {
    AudioCapture(
        maxDuration = 10_000L,
        onDismissRequest = {},
        onAudioCaptured = {}
    )
}
