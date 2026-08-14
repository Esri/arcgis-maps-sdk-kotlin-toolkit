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
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.FileUriReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

internal class AudioCaptureViewModel(
    val maxDuration: Long?,
    private val onAudioCaptured: suspend (File) -> Result<Unit>
) : ViewModel() {

    /**
     * The [MediaRecorder] instance used for audio recording. It is initialized in the [initialize]
     * method and released in the [onCleared] method.
     */
    private lateinit var recorder: MediaRecorder

    /**
     * The [FileUriReference] instance that holds the reference to the file where the audio recording
     * will be saved. It is expected to be initialized before starting the recording.
     */
    private lateinit var fileUriReference: FileUriReference

    /**
     * A flag indicating whether the audio recording is currently in progress.
     */
    private var _isRecording = mutableStateOf(false)

    private var timerJob: Job? = null

    /**
     * Backing state for [elapsedSeconds].
     */
    private var _elapsedSeconds = mutableLongStateOf(0L)

    /**
     * A callback function that is invoked when the audio capture is complete and the [onAudioCaptured]
     * callback has been executed.
     */
    private var onAudioCaptureCompleteAction: (() -> Unit)? = null

    /**
     * The elapsed time in seconds since the start of the audio recording. This value is updated
     * periodically during the recording process and can be used to display the recording duration
     * in the UI.
     */
    val elapsedSeconds: Long
        get() = _elapsedSeconds.longValue

    init {
        Log.e("TAG", "AudioCaptureViewModel:init: ")
        viewModelScope.launch {
            snapshotFlow {
                _isRecording.value
            }.collectLatest {
                if (it) {
                    _elapsedSeconds.longValue = 0
                    startRecordingTimer()
                }
            }
        }
    }

    /**
     * Initializes the view model and prepares it for audio recording. This method should be called
     * before starting the recording.
     *
     * @param context The [Context] required for the initialization.
     */
    fun initialize(context: Context) {
        // Return early if the recorder is already initialized
        if (::recorder.isInitialized) return
        viewModelScope.launch(Dispatchers.IO) {
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
                setOutputFile(fileUriReference.file.absolutePath)
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
                        stopRecording()
                    }
                }
                prepare()
            }
        }
    }

    /**
     * Starts the audio recording process. See [elapsedSeconds] for tracking the duration of the
     * recording. This method should be called after [initialize].
     */
    fun startRecording(): Result<Unit> = runCatching {
        require(::recorder.isInitialized) {
            "Recorder is not initialized. Call initialize() first."
        }
        try {
            recorder.start()
        } catch (e: IllegalStateException) {
            cleanup()
            throw e
        }

        _isRecording.value = true
    }

    /**
     * Stops the ongoing audio recording and returns a [Result] of the operation. If the recording
     * is successfully captured, the resulting file is added to the AttachmentsFormElement.
     */
    fun stopRecording(): Result<Unit> = runCatching {
        require(::recorder.isInitialized) {
            "Recorder is not initialized. Call initialize() first."
        }
        try {
            recorder.stop()
            _isRecording.value = false
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                onAudioCaptured(fileUriReference.file).onSuccess {
                    onAudioCaptureCompleteAction?.invoke()
                }
            }
        } catch (e: IllegalStateException) {
            cleanup()
            throw e
        }
    }

    /**
     * Cancels the ongoing audio recording, if any, and cleans up the associated resources. This
     * method should be called when the user decides to cancel the recording process.
     */
    fun cancelRecording() {
        try {
            if (::recorder.isInitialized) {
                recorder.stop()
                _isRecording.value = false
            }
        } catch (e: IllegalStateException) {
            Log.e("AudioCaptureViewModel", "Error stopping recorder: ${e.message}")
        } finally {
            cleanup()
        }
    }

    fun setOnAudioCaptureCompleteAction(action: (() -> Unit)?) {
        onAudioCaptureCompleteAction = action
    }

    override fun onCleared() {
        Log.e("TAG", "AudioCaptureViewModel::onCleared")
        super.onCleared()
        cleanup()
    }

    private fun cleanup() {
        timerJob?.cancel()
        if (::recorder.isInitialized) {
            recorder.release()
        }
        if (::fileUriReference.isInitialized) {
            fileUriReference.file.delete()
        }
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()
        _elapsedSeconds.longValue = 0L

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _elapsedSeconds.longValue = (System.currentTimeMillis() - startTime) / 1000L
            }
        }
    }

    companion object {
        fun Factory(
            maxDuration: Long?,
            onAudioCaptured: suspend (File) -> Result<Unit>
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AudioCaptureViewModel(
                    maxDuration = maxDuration,
                    onAudioCaptured = onAudioCaptured
                )
            }
        }
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
