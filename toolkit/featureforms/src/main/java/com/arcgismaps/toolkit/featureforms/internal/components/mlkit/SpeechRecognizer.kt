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

package com.arcgismaps.toolkit.featureforms.internal.components.mlkit

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import java.util.Locale

internal class SpeechRecognizer {

    private val speechRecognizer by lazy {
        // initialize the speech recognizer when needed
        SpeechRecognition.getClient(
            speechRecognizerOptions {
                locale = Locale.US
                preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
            }
        )
    }

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response.asStateFlow()

    suspend fun initialize() : Result<Unit> {
        when (val status = speechRecognizer.checkStatus()) {
            FeatureStatus.DOWNLOADABLE -> {
                speechRecognizer.download().takeWhile {
                    it !is DownloadStatus.DownloadCompleted
                }.collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadProgress -> {
                            Log.e("TAG", "progress: ${status.totalBytesDownloaded}")
                        }

                        else -> {

                        }
                    }
                }
                return if (speechRecognizer.checkStatus() == FeatureStatus.AVAILABLE) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to download speech recognizer"))
                }
            }

            FeatureStatus.AVAILABLE -> {
                return Result.success(Unit)
            }

            else -> {
                Log.e("TAG", "Speech recognizer is not available: $status")
                return Result.failure(Exception("Speech recognizer is not available"))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun startVoiceRecognition() {
        val request = speechRecognizerRequest {
            audioSource = AudioSource.fromMic()
        }
        speechRecognizer.startRecognition(request).collect { response ->
            when (response) {
                is SpeechRecognizerResponse.FinalTextResponse -> {
                    val previous = _response.value
                    _response.emit("$previous ${response.text}")
                }

                else -> {}
            }
        }
    }

    suspend fun stopVoiceRecognition() {
        speechRecognizer.stopRecognition()
    }

    fun resetResponse() {
        _response.value = ""
    }
}
