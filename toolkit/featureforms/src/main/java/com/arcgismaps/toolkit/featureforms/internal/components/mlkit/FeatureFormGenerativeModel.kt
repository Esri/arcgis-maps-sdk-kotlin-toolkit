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
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.flow.takeWhile

internal class FeatureFormGenerativeModel(
    private val prompt: FeatureFormPrompt
) {
    private val generativeModel by lazy {
        val config = generationConfig {
            modelConfig = modelConfig {
                preference = ModelPreference.FULL
            }
        }
        Generation.getClient(config)
    }

    suspend fun initialize(): Result<Unit> {
        val status = generativeModel.checkStatus()
        Log.e("TAG", "initialize prompt model: $status", )
        return when (status) {
            FeatureStatus.AVAILABLE -> Result.success(Unit)
            FeatureStatus.DOWNLOADABLE -> {
                generativeModel.download().takeWhile {
                    it !is DownloadStatus.DownloadCompleted
                }.collect { status ->
                    when(status) {
                        DownloadStatus.DownloadCompleted -> Log.e("TAG", "download completed")
                        is DownloadStatus.DownloadFailed -> {
                            Log.e("TAG", "download failed: ${status.e}")
                        }
                        is DownloadStatus.DownloadProgress -> {
                            Log.e("TAG", "progress: ${status.totalBytesDownloaded}")
                        }
                        is DownloadStatus.DownloadStarted -> {
                            Log.e("TAG", "initialize: started", )
                        }
                    }
                }
                val postDownloadStatus = generativeModel.checkStatus()
                if (postDownloadStatus == FeatureStatus.AVAILABLE) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Model is not available after download, status: $postDownloadStatus"))
                }
            }

            FeatureStatus.UNAVAILABLE -> Result.failure(Exception("Model is unavailable"))
            else -> Result.failure(Exception("Unknown model status: $status"))
        }
    }

    suspend fun getResponse(userPrompt: String): Result<Unit> {
        return try {
            val response = generativeModel.generateContent(
                generateContentRequest(TextPart(userPrompt)) {
                    promptPrefix = PromptPrefix(prompt.prompt.value)
                }
            )
            val content = response.candidates.firstOrNull()?.text ?: ""
            Log.e("TAG", "getResponse: $content")
            val cleanJson = content.extractJsonObject()
            FeatureFormPromptResponse.fromJsonOrNull(cleanJson)?.let {
                prompt.processResponse(it)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
