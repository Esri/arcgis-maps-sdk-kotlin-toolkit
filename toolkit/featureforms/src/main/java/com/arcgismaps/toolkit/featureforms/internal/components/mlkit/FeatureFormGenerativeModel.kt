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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A wrapper around ML Kit's generative model client.
 *
 * @param prefix The initial prompt to use as a prefix for all generations. This is cached and provided
 * as a contextual starting point for the model, allowing it to generate responses that are relevant to the
 * specific feature form use case.
 * @param onProcessedResponse A callback to handle the processed response from the model. This is
 * in a structured JSON format.
 */
internal class FeatureFormGenerativeModel(
    private val prefix: String,
    private val onProcessedResponse: (String) -> Unit
) : Loadable {
    private val generativeModel by lazy {
        val config = generationConfig {
            modelConfig = modelConfig {
                preference = ModelPreference.FULL
            }
        }
        Generation.getClient(config)
    }

    private val mutex = Mutex()

    private val promptPrefix = PromptPrefix(prefix)

    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.NotLoaded)

    override val loadStatus: StateFlow<LoadStatus> = _loadStatus.asStateFlow()

    /**
     * Sends a generation request to the model with the provided user prompt.
     *
     * @param userPrompt The input from the user that will be sent to the model for generating a response.
     * @return A [Result] indicating the success or failure of the generation process. On success,
     * the response is processed and passed to the [onProcessedResponse] callback.
     */
    suspend fun getResponse(userPrompt: String): Result<Unit> = runCatching {
        val request = generateContentRequest(TextPart(userPrompt)) {
            promptPrefix = this@FeatureFormGenerativeModel.promptPrefix
        }
        Log.e("TAG", "Tokens: ${generativeModel.countTokens(request).totalTokens}")
        val response = generativeModel.generateContent(request)
        val content = response.candidates.firstOrNull()?.text ?: ""
        Log.e("TAG", "getResponse: $content")
        val cleanJson = content.extractJsonObject()
        onProcessedResponse(cleanJson)
    }

    override suspend fun load(): Result<Unit> {
        return when (_loadStatus.value) {
            is LoadStatus.Loaded -> Result.success(Unit)
            is LoadStatus.Loading -> {
                _loadStatus.first {
                    it.isTerminal
                }.toResult()
            }

            is LoadStatus.NotLoaded, is LoadStatus.FailedToLoad -> {
                mutex.withLock {
                    // Check the load status again inside the mutex to prevent multiple loads.
                    if (_loadStatus.value is LoadStatus.NotLoaded || _loadStatus.value is LoadStatus.FailedToLoad) {
                        loadModel()
                    } else {
                        // If the status has already changed from NotLoaded, we can return success
                        // or failure based on the new status.
                        _loadStatus.value.toResult()
                    }
                }
            }
        }
    }

    override suspend fun retryLoad(): Result<Unit> = load()

    override fun cancelLoad() {
        /* No-op since MLKit does not have a cancellation mechanism */
    }

    /**
     * Loads the generative model. This is not thread-safe and should only be called under a locked
     * context to prevent multiple concurrent loads.
     *
     * @return A [Result] indicating the success or failure of the load operation.
     */
    private suspend fun loadModel(): Result<Unit> {
        _loadStatus.value = LoadStatus.Loading
        val status = generativeModel.checkStatus()
        Log.e("TAG", "initialize prompt model: $status")
        return when (status) {
            FeatureStatus.AVAILABLE -> {
                Result.success(Unit).also {
                    _loadStatus.value = LoadStatus.Loaded
                }
            }

            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                generativeModel.download().takeWhile {
                    it.isTerminal().not()
                }.collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadProgress -> {
                            Log.e("TAG", "progress: ${status.totalBytesDownloaded}")
                        }

                        is DownloadStatus.DownloadStarted -> {
                            Log.e("TAG", "initialize: started")
                        }

                        else -> {}
                    }
                }
                val postDownloadStatus = generativeModel.checkStatus()
                if (postDownloadStatus == FeatureStatus.AVAILABLE) {
                    generativeModel.warmup()
                    Result.success(Unit).also {
                        _loadStatus.value = LoadStatus.Loaded
                    }
                } else {
                    val ex =
                        Exception("Model is not available after download, status: $postDownloadStatus")
                    _loadStatus.value = LoadStatus.FailedToLoad(ex)
                    Result.failure(ex)
                }
            }

            else -> {
                val ex = Exception("Model is Unavailable")
                _loadStatus.value = LoadStatus.FailedToLoad(ex)
                Result.failure(ex)
            }
        }
    }
}

@Composable
internal fun rememberFeatureFormGenerativeModel(
    prefix: String,
    onProcessedResponse: (String) -> Unit
): FeatureFormGenerativeModel {
    return retain(prefix, onProcessedResponse) {
        Log.e("TAG", "rememberFeatureFormGenerativeModel: retained")
        FeatureFormGenerativeModel(prefix, onProcessedResponse)
    }
}

internal fun DownloadStatus.isTerminal(): Boolean {
    return this is DownloadStatus.DownloadCompleted || this is DownloadStatus.DownloadFailed
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

private fun LoadStatus.toResult(): Result<Unit> {
    return when (this) {
        is LoadStatus.Loaded -> Result.success(Unit)
        is LoadStatus.FailedToLoad -> Result.failure(this.error)
        else -> error("Expected terminal load status but was $this")
    }
}
