/*
 * Copyright 2024 Esri
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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.featureforms.AttachmentFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state of an [AttachmentFormElement]
 */
internal class AttachmentElementState(
    private val formElement: AttachmentFormElement,
    private val scope: CoroutineScope
) : FormElementState(
    label = formElement.label,
    description = formElement.description,
    isVisible = formElement.isVisible,
) {
    /**
     * The attachments associated with the form element.
     */
    val attachments = SnapshotStateList<FormAttachmentState>()

    /**
     * The state of the lazy list that displays the [attachments].
     */
    val lazyListState = LazyListState()

    val inputType = formElement.input

    init {
        scope.launch {
            loadAttachments()
        }
    }

    /**
     * Loads the attachments associated with the form element. This clears the current list of
     * attachments and updates it with the list of attachments from the [formElement].
     */
    private suspend fun loadAttachments() {
        formElement.fetchAttachments()
        attachments.clear()
        attachments.addAll(
            formElement.attachments.map {
                FormAttachmentState(it, scope)
            }
        )
    }

    /**
     * Adds an attachment with the given [name], [contentType], and [data].
     */
    suspend fun addAttachment(name: String, contentType: String, data: ByteArray) {
        formElement.addAttachment(name, contentType, data)
        // refresh the list of attachments
        loadAttachments()
        // load the attachment that was just added
        attachments.last().loadAttachment()
        // scroll to the newly added attachment
        lazyListState.scrollToItem(attachments.size - 1)
    }

    fun hasCameraPermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            scope: CoroutineScope
        ): Saver<AttachmentElementState, Any> = listSaver(
            save = {
                // save the list of indices of attachments that have been loaded
                buildList<Int> {
                    for (i in it.attachments.indices) {
                        if (it.attachments[i].loadStatus.value is LoadStatus.Loaded) {
                            add(i)
                        }
                    }
                }
            },
            restore = { savedList ->
                AttachmentElementState(attachmentFormElement, scope).also {
                    scope.launch {
                        it.loadAttachments()
                        // load the attachments that were previously loaded
                        savedList.forEach { index ->
                            it.attachments[index].loadAttachment()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Represents the state of a [FormAttachment].
 */
internal class FormAttachmentState(
    val name: String,
    val size: Long,
    val loadStatus: StateFlow<LoadStatus>,
    private val onLoadAttachment: suspend () -> Result<Unit>,
    private val onLoadThumbnail: suspend () -> Result<BitmapDrawable?>,
    private val scope: CoroutineScope
) {
    private val _thumbnail: MutableState<ImageBitmap?> = mutableStateOf(null)
    val thumbnail: State<ImageBitmap?> = _thumbnail

    val type: AttachmentType = getAttachmentType(name)

    constructor(attachment: FormAttachment, scope: CoroutineScope) : this(
        name = attachment.name,
        size = attachment.size,
        loadStatus = attachment.loadStatus,
        onLoadAttachment = attachment::load,
        onLoadThumbnail = attachment::createFullImage,
        scope = scope
    )

    fun loadAttachment() {
        scope.launch {
            onLoadAttachment().onSuccess {
                onLoadThumbnail().onSuccess {
                    if (it != null) {
                        _thumbnail.value = it.bitmap.asImageBitmap()
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentFormElement
): AttachmentElementState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = AttachmentElementState.Saver(attachmentFormElement, scope)
    ) {
        AttachmentElementState(
            formElement = attachmentFormElement,
            scope = scope
        )
    }
}

internal sealed class AttachmentType {
    data object Image : AttachmentType()
    data object Audio : AttachmentType()
    data object Video : AttachmentType()
    data object Document : AttachmentType()
    data object Other : AttachmentType()
}

internal fun getAttachmentType(filename: String): AttachmentType {
    val extension = filename.substring(filename.lastIndexOf(".") + 1)
    return when (extension) {
        "jpg", "jpeg", "png", "gif", "bmp" -> AttachmentType.Image
        "mp3", "wav", "ogg", "flac" -> AttachmentType.Audio
        "mp4", "avi", "mov", "wmv", "flv" -> AttachmentType.Video
        "doc", "docx", "pdf", "txt", "rtf" -> AttachmentType.Document
        else -> AttachmentType.Other
    }
}

@Composable
internal fun AttachmentType.getIcon(): ImageVector = when (this) {
    AttachmentType.Image -> Icons.Outlined.Image
    AttachmentType.Audio -> Icons.Outlined.AudioFile
    AttachmentType.Video -> Icons.Outlined.VideoCameraBack
    AttachmentType.Document -> Icons.Outlined.FilePresent
    AttachmentType.Other -> Icons.Outlined.FileCopy
}
