/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.element.attachment

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.popup.AttachmentsPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupAttachment
import com.arcgismaps.mapping.popup.PopupAttachmentType
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state of an [AttachmentsPopupElement]
 */
@Immutable
internal class AttachmentsElementState(
    val description: String,
    val title: String,
    val attachments: List<PopupAttachmentState>,
    override val id: Int = createId(),
) : PopupElementState() {

    constructor(attachmentPopupElement: AttachmentsPopupElement) : this(
        description = attachmentPopupElement.description,
        title = attachmentPopupElement.title,
        attachments = attachmentPopupElement.attachments.map { PopupAttachmentState(it) }
    )

    companion object {
        fun Saver(
            element: AttachmentsPopupElement
        ): Saver<AttachmentsElementState, Any> = Saver(
            save = { null },
            restore = {
                AttachmentsElementState(
                    element
                )
            }
        )

    }
}

@Composable
internal fun rememberAttachmentsElementState(
    element: AttachmentsPopupElement,
    popup: Popup
): AttachmentsElementState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        inputs = arrayOf(popup, element),
        saver = AttachmentsElementState.Saver(element)
    ) {
        AttachmentsElementState(
            element
        ).also {
            // NOTE: core issue with PopupAttachments not abiding the instance id contract here.
            // Loaded attachments are coming back NotLoaded after rotation.
            // https://devtopia.esri.com/runtime/apollo/issues/681
            it.attachments
                .filter { state ->
                    state.loadStatus.value == LoadStatus.Loaded
                            && state.popupAttachmentType == PopupAttachmentType.Image
                }
                .forEach { state ->
                    state.loadAttachment(scope)
                }
        }
    }
}

/**
 * Represents the state of a [PopupAttachment].
 *
 * @param name The name of the attachment.
 * @param size The size of the attachment.
 * @param loadStatus The load status of the attachment.
 * @param onLoadAttachment A function that loads the attachment.
 */
internal class PopupAttachmentState(
    val name: String,
    val size: Long,
    val popupAttachmentType: PopupAttachmentType,
    val contentType: String,
    val loadStatus: StateFlow<LoadStatus>,
    private val onLoadAttachment: suspend () -> Result<Unit>,
) {

    private lateinit var _attachment: PopupAttachment

    private val _thumbnailUri: MutableState<String> = mutableStateOf("")
    /**
     * The URI of the attachment image. Empty until [loadAttachment] is called.
     */
    val thumbnailUri: State<String> = _thumbnailUri
    val path: String
        get() = _attachment.filePath

    constructor(attachment: PopupAttachment) : this(
        name = attachment.name,
        size = attachment.size,
        popupAttachmentType = attachment.type,
        contentType = attachment.contentType,
        loadStatus = attachment.loadStatus,
        onLoadAttachment = attachment::retryLoad
    ) {
        _attachment = attachment
    }

    /**
     * Loads the attachment and its thumbnail.
     */
    fun loadAttachment(scope: CoroutineScope) {
        scope.launch {
            onLoadAttachment().onSuccess {
                _thumbnailUri.value = _attachment.filePath
            }
        }
    }
}

@Composable
internal fun PopupAttachmentType.getIcon(): ImageVector = when (this) {
    PopupAttachmentType.Image -> Icons.Outlined.Image
    PopupAttachmentType.Video -> Icons.Outlined.VideoCameraBack
    PopupAttachmentType.Document -> Icons.Outlined.FilePresent
    PopupAttachmentType.Other -> Icons.Outlined.FileCopy
}
