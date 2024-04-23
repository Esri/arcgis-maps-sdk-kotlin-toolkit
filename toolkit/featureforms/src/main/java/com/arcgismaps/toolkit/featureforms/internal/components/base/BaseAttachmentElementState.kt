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

package com.arcgismaps.toolkit.featureforms.internal.components.base

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.exifinterface.media.ExifInterface
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.AttachmentFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogRequester
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A state object for an individual [FormAttachment].
 *
 * @property name the name of the attachment
 * @property size the size of the attachment
 * @property rename a lambda to rename the attachment
 * @property createFullImage a lambda to create the full size image thumbnail of the attachment.
 * @property getFilePath a lambda to provide the local file path to the attachment. This path is non-null
 * after the FormAttachment is loaded.
 * @property loadStatus the FormAttachment's loadStatus StateFlow
 * @property loadAttachment loads the FormAttachment
 * @property cancelAttachmentLoad cancels the FormAttachment load
 */
internal class AttachmentState(
    val name: String,
    val size: Long,
    val rename: suspend (String) -> Result<Unit>,
    private val createFullImage: suspend () -> Result<BitmapDrawable>,
    val getFilePath: () -> String?,
    override val loadStatus: StateFlow<LoadStatus>,
    val loadAttachment: suspend () -> Result<Unit>,
    val cancelAttachmentLoad: () -> Unit,

    ): Loadable {
    /**
     * A lambda to invoke the viewer
     */
    var viewAttachment: ((DialogRequester) -> Unit)? = null

    /**
     * The full scale image of the FormAttachment
     */
    var image: Painter? = null

    constructor(formAttachment: FormAttachment, element: AttachmentFormElement) : this(
        name = formAttachment.name,
        size = formAttachment.size,
        rename = { str: String ->
            element.renameAttachment(formAttachment, str)
        },
        createFullImage = suspend {
            formAttachment.createFullImage()
        },
        getFilePath = { formAttachment.filePath },
        loadStatus = formAttachment.loadStatus,
        loadAttachment = suspend {
            formAttachment.retryLoad()
        },
        cancelAttachmentLoad = { formAttachment.cancelLoad() }

    ) {
        viewAttachment = {
            it.requestDialog(
                DialogType.AttachmentViewerDialog(image!!)
            )
        }
    }

    internal suspend fun getThumbnail(): Result<Painter> {
        if (image != null) return Result.success(image!!)
        return when (loadStatus.value) {
            is LoadStatus.Loaded -> {
                createFullImage.invoke().map {
                    BitmapPainter(
                        it.bitmap.rotateIfNecessary(getFilePath()!!)
                            .asImageBitmap()
                    ).also { painter ->
                        image = painter
                    }
                }
            }
            else -> Result.failure(IllegalStateException("attachment must be loaded to access its thumbnail"))
        }
    }

    override suspend fun load(): Result<Unit> = loadAttachment()

    override suspend fun retryLoad(): Result<Unit> = loadAttachment()

    override fun cancelLoad() = cancelAttachmentLoad()
}

/**
 * AttachmentFormElement state object.
 *
 * @property emitAttachments a lambda to cause the attachments list to be emitted by the [attachments]
 * flow
 * @property fetchAttachments a lambda to repopulate the AttachmentFormElements attachments list
 * @property addAttachment a lambda to add an attachment
 * @property label the AttachmentFormElement label
 * @property description the AttachmentFormElement description
 * @property isVisible the AttachmentFormElement isVisible flow
 * @parameter scope a coroutine scope to handle concurrent work
 *
 *
 */
internal class BaseAttachmentElementState(
    private val emitAttachments: () -> List<AttachmentState>,
    private val fetchAttachments: suspend () -> Result<Unit>,
    private val addAttachment: suspend (String, String, ByteArray)-> Result<Unit>,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    scope: CoroutineScope
) : FormElementState(
    label = label,
    description = description,
    isVisible = isVisible
) {
    private val _attachmentsFlow = MutableStateFlow<List<AttachmentState>>(emptyList())
    val attachments = _attachmentsFlow.asStateFlow()
    init {
        scope.launch {
            fetch()
        }
    }

    constructor(attachmentFormElement: AttachmentFormElement, scope: CoroutineScope): this(
        emitAttachments = {
            attachmentFormElement.attachments.map {
                AttachmentState(it, attachmentFormElement)
            }
        },
        fetchAttachments = suspend {
            attachmentFormElement.fetchAttachments()
        },
        addAttachment = { name, contentType, bytes ->
            attachmentFormElement.addAttachment(name, contentType, bytes).map {
                it.load()
            }
        },
        label = attachmentFormElement.label,
        description = attachmentFormElement.description,
        isVisible = attachmentFormElement.isVisible,
        scope = scope
    )

    private fun emit() {
        _attachmentsFlow.value = emitAttachments.invoke()
    }
    
    private suspend fun fetch(): Result<Unit> =
        fetchAttachments.invoke().map {
            emit()
        }

    suspend fun addFormAttachment(name: String, contentType: String, bytes: ByteArray): Result<Unit> =
        addAttachment.invoke(name, contentType, bytes).map {
            fetch()
        }

    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            scope: CoroutineScope
        ): Saver<BaseAttachmentElementState, Any> = Saver(
            save = {},
            restore = {
                BaseAttachmentElementState(
                    attachmentFormElement = attachmentFormElement,
                    scope = scope
                )
            }
        )
    }
}

@Composable
internal fun rememberBaseAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentFormElement
): BaseAttachmentElementState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = BaseAttachmentElementState.Saver(attachmentFormElement, scope)
    ) {
        BaseAttachmentElementState(
            attachmentFormElement = attachmentFormElement,
            scope = scope
        )
    }
}


private fun Bitmap.rotateIfNecessary(filePath: String): Bitmap {
    val rotationAngle = findBitmapOrientation(filePath)
    return if (rotationAngle == 0) {
        this
    } else {
        rotateBitmap(rotationAngle)
    }
}

private fun findBitmapOrientation(filePath: String): Int = 0

private fun Bitmap.rotateBitmap(rotationAngle: Int): Bitmap {
    if (rotationAngle == 0)
        return this
    val matrix = Matrix()
    matrix.postRotate(rotationAngle.toFloat())
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

