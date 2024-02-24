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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.api.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.api.FormAttachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class BaseAttachmentElementState(
    val feature: ArcGISFeature,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    val attachments: StateFlow<List<FormAttachment>>,
    var selectedAttachment: FormAttachment? = null,
    private val onAttachmentsUpdated: () -> Unit = {}
) : FormElementState(
    label = label,
    description = description,
    isVisible = isVisible
) {
    
    fun attachmentsUpdated() {
        onAttachmentsUpdated()
    }
    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            feature: ArcGISFeature,
            onAttachmentsUpdated: () -> Unit
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
                    isVisible = attachmentFormElement.isVisible,
                    attachments = attachmentFormElement.attachments,
                    selectedAttachment = if (it.isNotEmpty()) {
                        it[0] as FormAttachment
                    } else {
                        null
                    },
                    onAttachmentsUpdated = onAttachmentsUpdated
                )
            }
        )
    }
}

internal fun formAttachmentFlow(
    scope: CoroutineScope,
    attachments: StateFlow<List<Attachment>>,
    filesDir: String
): StateFlow<List<FormAttachment>> {
    println ("TAG setting up form attachmet flow with ${attachments.value.size} attachments")
    val initialValues = attachments.value.map { FormAttachment(it, filesDir) }
    return attachments.map {
        it.map { attachment ->
            println ("TAG getting a formattachment for ${attachment.name}")
            FormAttachment(attachment, filesDir)
        }
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        initialValues
    )
}


@Composable
internal fun rememberBaseAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentFormElement,
    onAttachmentsUpdated: () -> Unit
): BaseAttachmentElementState {
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = BaseAttachmentElementState.Saver(attachmentFormElement, form.feature, onAttachmentsUpdated)
    ) {
        BaseAttachmentElementState(
            feature = form.feature,
            label = attachmentFormElement.label,
            description = attachmentFormElement.description,
            isVisible = attachmentFormElement.isVisible,
            attachments = attachmentFormElement.attachments,
            selectedAttachment = null,
            onAttachmentsUpdated = onAttachmentsUpdated
        )
    }
}
