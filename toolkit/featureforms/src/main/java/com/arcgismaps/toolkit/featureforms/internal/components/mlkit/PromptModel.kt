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
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.takeWhile

internal class PromptModel(
    private val prefix : String
) {

    private val generativeModel by lazy {
        Generation.getClient()
    }

    private val promptPrefix by lazy {
        PromptPrefix(prefix)
    }

    suspend fun initialize() : Result<Unit> {
        return when (val status = generativeModel.checkStatus()) {
            FeatureStatus.AVAILABLE -> Result.success(Unit)
            FeatureStatus.DOWNLOADABLE -> {
                generativeModel.download().takeWhile {
                    it !is DownloadStatus.DownloadCompleted
                }.collect {}
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

    suspend fun getResponse(prompt: String): Result<String> {
        return try {
            val response = generativeModel.generateContent(
                generateContentRequest(TextPart(prompt)) {
                    promptPrefix = this@PromptModel.promptPrefix
                }
            )
            Result.success(response.candidates.first().text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
