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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.api.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.api.FormAttachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class BaseAttachmentElementState(
    val feature: ArcGISFeature,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    scope: CoroutineScope,
    var selectedAttachment: FormAttachment? = null,
    val filesDir: String
    //private val onAttachmentsUpdated: () -> Unit = {}
) : FormElementState(
    label = label,
    description = description,
    isVisible = isVisible
) {
    private val _attachments = mutableListOf<FormAttachment>()
    private val _attachmentsFlow = MutableStateFlow<List<FormAttachment>>(emptyList())
    val attachments = _attachmentsFlow.asStateFlow()
    
    private val mutex = Mutex()
    init {
        scope.launch {
            fetchAttachments()
        }
    }
    
    suspend fun fetchAttachments(): Result<Unit> {
        mutex.withLock {
            val featureAttachments = feature.fetchAttachments().onFailure {
                return Result.failure(it)
            }.getOrNull()!!
            val formAttachments = featureAttachments.map {
                FormAttachment(it, filesDir)
            }
            _attachments.clear()
            _attachments.addAll(formAttachments)
            _attachmentsFlow.value = _attachments.toList()
            return Result.success(Unit)
        }
    }
    
    suspend fun addAttachment(
        name: String,
        contentType: String,
        bytes: ByteArray
    ): Result<FormAttachment> {
        val attachment = feature.addAttachment(name, contentType, bytes).getOrNull()
        return if (attachment != null) {
            val formAttachment = FormAttachment(attachment, filesDir)
            formAttachment.load()
            _attachments.add(formAttachment)
            _attachmentsFlow.value = _attachments.toList()
            Result.success(formAttachment)
        } else {
            Result.failure(Exception("Unable to create attachment"))
        }
    }
    
    
    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            feature: ArcGISFeature,
            scope: CoroutineScope,
            visibleProxy: StateFlow<Boolean>,
            filesDir: String
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
                    label = attachmentFormElement.label,
                    description = attachmentFormElement.description,
                    isVisible = visibleProxy,
                    selectedAttachment = if (it.isNotEmpty()) {
                        it[0] as FormAttachment
                    } else {
                        null
                    },
                    scope = scope,
                    filesDir = filesDir
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
    val filesDir = LocalContext.current.filesDir.absolutePath
    val isVisibleProxyElement = form.elements.filterIsInstance<FieldFormElement>().firstOrNull() {
        it.label == "visual_defects"
    }
    
    val visibleProxy = isVisibleProxyElement?.value?.map { if (it is String) {
        it == "Yes"
    } else {
        false
    }
    }?.stateIn(scope, SharingStarted.Eagerly, false)
        ?: MutableStateFlow<Boolean>(false).asStateFlow()
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = BaseAttachmentElementState.Saver(attachmentFormElement, form.feature, scope, visibleProxy, filesDir)
    ) {
        BaseAttachmentElementState(
            feature = form.feature,
            label = attachmentFormElement.label,
            description = attachmentFormElement.description,
            isVisible = visibleProxy,
            selectedAttachment = null,
            scope = scope,
            filesDir = filesDir
        )
    }
}
