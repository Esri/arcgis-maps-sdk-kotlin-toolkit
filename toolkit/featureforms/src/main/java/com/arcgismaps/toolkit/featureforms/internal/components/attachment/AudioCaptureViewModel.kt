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
import android.os.SystemClock
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.FileUriReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * Represents the state of the audio capture process.
 */
internal sealed class AudioCaptureState {

    /**
     * Indicates that the audio capture is not ready to start recording. This state represents the
     * initial state before the audio capture is initialized and prepared for recording.
     */
    object NotReady : AudioCaptureState()

    /**
     * Indicates that the audio capture is ready to start recording.
     */
    object Ready : AudioCaptureState()

    /**
     * Indicates that the audio capture is currently recording audio.
     *
     * See [AudioCaptureViewModel.elapsedSeconds] for tracking the duration of the recording.
     */
    object Recording : AudioCaptureState()

    /**
     * Indicates that the audio capture is currently saving the recorded audio to a file.
     */
    object Saving : AudioCaptureState()

    /**
     * Indicates that the audio capture has stopped recording and is no longer active. This state
     * is the final state and the audio capture cannot be reused for another recording session.
     */
    object Stopped : AudioCaptureState()

    /**
     * Indicates that an error occurred during the audio capture process. The [message] provides
     * details about the error that occurred.
     *
     * @param message A string describing the error that occurred during audio capture.
     */
    data class Error(val message: String) : AudioCaptureState()
}

/**
 * A viewmodel that manages a single audio recording session. [initialize] must be called before
 * starting the recording. This only supports a single recording session and hence cannot be reused
 * for multiple recordings. Create a new instance of this viewmodel for each recording session.
 *
 * [startRecording] starts the recording and [stopRecording] stops the recording. [onAudioCaptured]
 * is called when the recording is stopped and the audio file is successfully captured. This can be
 * used to process the captured audio file, such as adding it to an attachment form element. The
 * [maxDuration] parameter can be used to specify the maximum duration of the recording in seconds.
 *
 * @param maxDuration The maximum duration of the recording in seconds. If null, there is no limit.
 * This must be greater than 1 second, if specified.
 * @param onAudioCaptured A suspend function that is called when the audio is captured. It receives
 * the captured audio file and returns a Result indicating success or failure.
 */
internal class AudioCaptureViewModel(
    val maxDuration: Long?,
    private val onAudioCaptured: suspend (File) -> Result<Unit>
) : ViewModel() {

    /**
     * The [MediaRecorder] instance used for audio recording. It is initialized in the [initialize]
     * method and released in the [onCleared] method.
     */
    private var recorder: MediaRecorder? = null

    /**
     * The [FileUriReference] instance that holds the reference to the file where the audio recording
     * will be saved. It is expected to be initialized before starting the recording.
     */
    private var fileUriReference: FileUriReference? = null

    /**
     * A [Job] that represents the coroutine responsible for tracking the elapsed time of the audio
     * recording.
     */
    private var timerJob: Job? = null

    /**
     * A [Mutex] used to synchronize access to the audio recording process.
     */
    private val mutex = Mutex()

    /**
     * Backing state for [elapsedSeconds].
     */
    private var _elapsedSeconds = mutableLongStateOf(0L)

    /**
     * The elapsed time in seconds since the start of the audio recording. This value is updated
     * periodically during the recording process and can be used to display the recording duration
     * in the UI.
     */
    val elapsedSeconds: Long
        get() = _elapsedSeconds.longValue

    /**
     * Backing property for [state].
     */
    private var _state = mutableStateOf<AudioCaptureState>(AudioCaptureState.NotReady)

    /**
     * The current state of the audio capture process. This state can be used to determine the
     * readiness of the audio capture, whether it is recording, saving, stopped, or if an error has
     * occurred. The state is updated based on the actions performed in the view model.
     */
    val state: AudioCaptureState
        get() = _state.value

    /**
     * Initializes the view model and prepares it for audio recording. This method should be called
     * before starting the recording.
     *
     * @param context The [Context] required for the initialization.
     */
    fun initialize(context: Context) = viewModelScope.launch {
        // Ensure init is thread safe
        mutex.withLock {
            // Return early if the recorder has already been initialized
            if (_state.value.isInitialised()) {
                return@launch
            }
            var candidateReference: FileUriReference? = null
            var candidateRecorder: MediaRecorder? = null
            try {
                withContext(Dispatchers.IO) {
                    val timeStamp = Instant.now().toEpochMilli()
                    candidateReference = AttachmentsFileProvider.createTempFileWithUri(
                        "AUDIO_$timeStamp",
                        ".m4a",
                        context
                    )
                    candidateRecorder = context.createMediaRecorder()
                    candidateRecorder.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(candidateReference.file.absolutePath)
                        maxDuration?.let { duration ->
                            if (duration > 1) {
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
                // Check against cancellation
                ensureActive()
                fileUriReference = checkNotNull(candidateReference)
                recorder = checkNotNull(candidateRecorder)
                // Set the local vars to null to avoid releasing them in the finally block
                candidateRecorder = null
                candidateReference = null
                // Update the state to Ready after successful initialization
                _state.value = AudioCaptureState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AudioCaptureState.Error(
                    message = "Failed to initialize audio recorder: ${e.message}"
                )
            } finally {
                // Release the candidate recorder and delete the candidate file if they were not
                // used
                candidateRecorder?.release()
                candidateReference?.file?.deleteIfExists()
            }
        }
    }

    /**
     * Starts the audio recording process. See [elapsedSeconds] for tracking the duration of the
     * recording. This method should be called after [initialize].
     */
    fun startRecording() = runCatching {
        require(_state.value is AudioCaptureState.Ready) {
            "Audio recorder is not ready."
        }
        val activeRecorder = checkNotNull(recorder) {
            "Recorder is not available."
        }
        activeRecorder.start()
        startRecordingTimer()
        _state.value = AudioCaptureState.Recording
    }.onFailure { exception ->
        _state.value = AudioCaptureState.Error(
            exception.message ?: "Failed to start audio recording."
        )
        cleanup()
    }

    /**
     * Stops the ongoing audio recording and returns a [Result] of the operation. If the recording
     * is successfully captured, the resulting file is added to the AttachmentsFormElement.
     */
    fun stopRecording() = runCatching {
        require(_state.value is AudioCaptureState.Recording) {
            "Audio recording is not active."
        }
        val activeRecorder = checkNotNull(recorder) {
            "Recorder is not available."
        }
        _state.value = AudioCaptureState.Saving
        timerJob?.cancel()
        timerJob = null
        activeRecorder.stop()

        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            onAudioCaptured(fileUriReference!!.file).onSuccess {
                _state.value = AudioCaptureState.Stopped
            }.onFailure {
                _state.value = AudioCaptureState.Error(
                    message = "Failed to save audio recording: ${it.message}"
                )
            }
        }
    }.onFailure { exception ->
        _state.value = AudioCaptureState.Error(
            exception.message ?: "Failed to stop audio recording."
        )
        cleanup()
    }

    /**
     * Cancels the ongoing audio recording, if any, and cleans up the associated resources. This
     * method should be called when the user decides to cancel the recording process.
     */
    fun cancelRecording() {
        try {
            if (_state.value is AudioCaptureState.Recording) {
                recorder?.stop()
                _state.value = AudioCaptureState.Stopped
            }
        } catch (e: Exception) {
            _state.value = AudioCaptureState.Error(
                message = "Failed to cancel audio recording: ${e.message}"
            )
        } finally {
            cleanup()
        }
    }

    override fun onCleared() {
        cleanup()
        super.onCleared()
    }

    private fun cleanup() {
        timerJob?.cancel()
        timerJob = null
        if (recorder != null) {
            recorder!!.release()
            recorder = null
        }
        if (fileUriReference != null) {
            fileUriReference!!.file.delete()
            fileUriReference = null
        }
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        val startTime = SystemClock.elapsedRealtime()
        _elapsedSeconds.longValue = 0L

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _elapsedSeconds.longValue = (SystemClock.elapsedRealtime() - startTime) / 1000L
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

/**
 * Indicates if the state has been initialised.
 */
private fun AudioCaptureState.isInitialised(): Boolean = when (this) {
    AudioCaptureState.NotReady -> false
    else -> true
}
