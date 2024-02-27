/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.base

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.mapping.featureforms.AttachmentFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class BaseAttachmentElementState(
    val feature: ArcGISFeature,
    val attachmentElement: AttachmentFormElement,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    scope: CoroutineScope,
    var selectedAttachment: FormAttachment? = null
) : FormElementState(
    label = label,
    description = description,
    isVisible = isVisible
) {
    private val _attachmentsFlow = MutableStateFlow<List<FormAttachment>>(emptyList())
    val attachments = _attachmentsFlow.asStateFlow()
    init {
        scope.launch {
            fetchAttachments()
        }
    }
    
    private suspend fun fetchAttachments(): Result<Unit> {
        return attachmentElement.fetchAttachments().map {
            _attachmentsFlow.value = attachmentElement.attachments
        }
    }
    
    private suspend fun fetchAttachments(newAttachment: Attachment): Result<Unit> {
        return attachmentElement.fetchAttachments().map {
            val attachments = attachmentElement.attachments
            attachments.firstOrNull { it.attachment?.name == newAttachment.name }?.load()
                ?.onFailure {
                    Log.d("ATTACH", "unable to load new attachment")
                } ?: Log.d("ATTACH", "unable to find new attachment to load")
            _attachmentsFlow.value = attachments
        }
    }
    
    suspend fun addAttachment(
        name: String,
        contentType: String,
        bytes: ByteArray
    ): Result<Unit> {
        val attachment = feature.addAttachment(name, contentType, bytes).getOrElse {
            Log.d("ATTACH", "failed to add add attachment with ${it.message}")
            Log.d("ATTACH", "addAttachment parameters name: $name, contentType: $contentType, bytes: $bytes")
            return Result.failure(it)
            
        }
        return fetchAttachments(attachment)
    }
    
    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            feature: ArcGISFeature,
            scope: CoroutineScope
        ): Saver<BaseAttachmentElementState, Any> = listSaver(
            save = {
                if (it.selectedAttachment != null) {
                    listOf(it.selectedAttachment)
                } else {
                    listOf()
                }
            },
            restore = {
                BaseAttachmentElementState(
                    feature,
                    attachmentElement  = attachmentFormElement,
                    label = attachmentFormElement.label,
                    description = attachmentFormElement.description,
                    isVisible = attachmentFormElement.isVisible,
                    selectedAttachment = if (it.isNotEmpty()) {
                        it[0] as FormAttachment
                    } else {
                        null
                    },
                    scope = scope
                )
            }
        )
    }
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
internal fun rememberBaseAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentFormElement
): BaseAttachmentElementState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = BaseAttachmentElementState.Saver(attachmentFormElement, form.feature, scope)
    ) {
        BaseAttachmentElementState(
            feature = form.feature,
            attachmentElement = attachmentFormElement,
            label = attachmentFormElement.label,
            description = attachmentFormElement.description,
            isVisible = attachmentFormElement.isVisible,
            selectedAttachment = null,
            scope = scope
        )
    }
}
