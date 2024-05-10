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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.AttachmentFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Objects

/**
 * Represents the state of an [AttachmentFormElement]
 *
 * @param formElement The form element that this state represents.
 * @param scope The coroutine scope used to launch coroutines.
 * @param evaluateExpressions A method to evaluates the expressions in the form.
 */
internal class AttachmentElementState(
    id: Int,
    private val formElement: AttachmentFormElement,
    private val scope: CoroutineScope,
    private val evaluateExpressions: suspend () -> Unit,
    private val filesDir: String
) : FormElementState(
    id = id,
    label = formElement.label,
    description = formElement.description,
    isVisible = formElement.isVisible
) {
    /**
     * The attachments associated with the form element.
     */
    val attachments = SnapshotStateList<FormAttachmentState>()

    /**
     * Indicates whether the attachment form element is editable.
     */
    val isEditable = formElement.isEditable

    /**
     * The state of the lazy list that displays the [attachments].
     */
    val lazyListState = LazyListState()

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
        formElement.attachments.forEach { formAttachment ->

        }
        attachments.addAll(
            formElement.attachments.map {
                FormAttachmentState(
                    name = it.name,
                    size = it.size,
                    contentType = it.contentType,
                    elementStateId = id,
                    deleteAttachment = { deleteAttachment(it) },
                    filesDir = filesDir,
                    scope = scope,
                    formAttachment = it
                )
            }
        )
    }

    /**
     * Adds an attachment with the given [name], [contentType], and [data].
     */
    suspend fun addAttachment(name: String, contentType: String, data: ByteArray) {
        formElement.addAttachment(name, contentType, data)
        evaluateExpressions()
        // refresh the list of attachments
        loadAttachments()
        // load the attachment that was just added
        attachments.last().loadWithParentScope()
        // scroll to the newly added attachment
        lazyListState.scrollToItem(attachments.size - 1)
    }

    private suspend fun deleteAttachment(formAttachment: FormAttachment) {
        formElement.deleteAttachment(formAttachment)
        val state = attachments.find { it.name == formAttachment.name } ?: return
        attachments.remove(state)
    }

    suspend fun renameAttachment(name: String, newName: String) {
        val formAttachment = formElement.attachments.firstOrNull { it.name == name } ?: return
        if (formAttachment.name != newName) {
            formElement.renameAttachment(formAttachment, newName)
            loadAttachments()
        }
    }

    fun hasCameraPermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun Saver(
            attachmentFormElement: AttachmentFormElement,
            scope: CoroutineScope,
            evaluateExpressions: suspend () -> Unit,
            filesDir: String
        ): Saver<AttachmentElementState, Any> = listSaver(
            save = {
                // save the list of indices of attachments that have been loaded
                buildList<Int> {
                    for (i in it.attachments.indices) {
                        if (it.attachments[i].loadStatus.value is LoadStatus.Loaded) {
                            add(i)
                        }
                    }
                    // save the index of the first visible item
                    add(it.lazyListState.firstVisibleItemIndex)
                    add(it.lazyListState.firstVisibleItemScrollOffset)
                }
            },
            restore = { savedList ->
                AttachmentElementState(
                    id = attachmentFormElement.hashCode(),
                    formElement = attachmentFormElement,
                    scope = scope,
                    evaluateExpressions = evaluateExpressions,
                    filesDir = filesDir
                ).also {
                    scope.launch {
                        it.loadAttachments()
                        // load the attachments that were previously loaded
                        for (i in savedList.dropLast(2)) {
                            it.attachments[i].loadWithParentScope()
                        }
                        // scroll to the last visible item
                        val firstVisibleItemIndex = savedList[savedList.count() - 2]
                        val firstVisibleItemScrollOffset = savedList[savedList.count() - 1]
                        it.lazyListState.scrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
                    }
                }
            }
        )
    }
}

/**
 * Represents the state of a [FormAttachment].
 *
 * @param name The name of the attachment.
 * @param size The size of the attachment.
 * @param contentType The content type of the attachment.
 * @param elementStateId The ID of the [AttachmentElementState] that created this attachment.
 * @param deleteAttachment A function to delete the attachment.
 * @param filesDir The directory where the attachments are stored.
 * @param scope The coroutine scope used to launch coroutines.
 * @param formAttachment The [FormAttachment] that this state represents.
 */
internal class FormAttachmentState(
    val name: String,
    val size: Long,
    val contentType: String,
    val elementStateId: Int,
    val deleteAttachment: suspend () -> Unit,
    private val filesDir: String,
    private val scope: CoroutineScope,
    private val formAttachment: FormAttachment? = null
) : Loadable {

    private val _thumbnail: MutableState<ImageBitmap?> = mutableStateOf(null)

    /**
     * The thumbnail of the attachment. This is `null` until [load] is called.
     */
    val thumbnail: State<ImageBitmap?> = _thumbnail

    /**
     * The type of the attachment.
     */
    val type: AttachmentType = getAttachmentType(name)

    private val _loadStatus: MutableStateFlow<LoadStatus> = MutableStateFlow(LoadStatus.NotLoaded)
    override val loadStatus = _loadStatus.asStateFlow()

    /**
     * The file path of the attachment on disk. This is empty until [load] is called.
     */
    var filePath: String = ""
        private set

    /**
     * The directory where the attachments are stored as defined in the [AttachmentsFileProvider].
     */
    private val attachmentsDir = "feature_forms_attachments"

    /**
     * Loads the attachment and its thumbnail in the coroutine scope of the state object that
     * created this attachment. Usually, this is the [AttachmentElementState] that created this
     * within the CoroutineScope of the root Feature Form composable.
     */
    fun loadWithParentScope() {
        scope.launch {
            load()
        }
    }

    /**
     * Loads the attachment and its thumbnail. Use [loadWithParentScope] to load the attachment as
     * a long-running task. This coroutine will get cancelled if the calling composable is removed
     * from the composition.
     */
    override suspend fun load(): Result<Unit> {
        _loadStatus.value = LoadStatus.Loading
        var result = Result.success(Unit)
        try {
            if (formAttachment == null) {
                result = Result.failure(Exception("Form attachment is null"))
            } else {
                formAttachment.retryLoad().onFailure {
                    result = Result.failure(it)
                }.onSuccess {
                    val data = formAttachment.attachment?.fetchData()?.getOrNull()
                    if (data != null) {
                        formAttachment.createFullImage().onSuccess {
                            _thumbnail.value = it.bitmap.asImageBitmap()
                        }
                        // write the data to disk only if the file does not exist
                        if (!File(filePath).exists()) {
                            writeDataToDisk(data)
                        }
                    } else {
                        result = Result.failure(Exception("Failed to load attachment data"))
                    }
                }
            }
        } catch (ex: CancellationException) {
            result = Result.failure(ex)
            throw ex
        } catch (ex : Exception) {
            result = Result.failure(ex)
        }
        finally {
            if (result.isSuccess) {
                _loadStatus.value = LoadStatus.Loaded
            } else {
                val error = result.exceptionOrNull() ?: Exception("Failed to load attachment")
                _loadStatus.value = LoadStatus.FailedToLoad(error)
            }
        }
        return result
    }

    override fun cancelLoad() {
        formAttachment?.cancelLoad()
    }

    override suspend fun retryLoad(): Result<Unit> {
        return formAttachment?.retryLoad()
            ?: return Result.failure(Exception("Form attachment is null"))
    }

    override fun hashCode(): Int {
        return Objects.hash(name, size, type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormAttachmentState

        if (name != other.name) return false
        if (size != other.size) return false
        if (type != other.type) return false

        return true
    }

    private suspend fun writeDataToDisk(data: ByteArray) = withContext(Dispatchers.IO) {
        val directory = File(filesDir, attachmentsDir)
        directory.mkdirs()
        // write the data to disk
        val file = File(directory, name)
        file.createNewFile()
        FileOutputStream(file).use {
            it.write(data)
        }
        filePath = file.absolutePath
    }
}

@Composable
internal fun rememberAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentFormElement
): AttachmentElementState {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = AttachmentElementState.Saver(
            attachmentFormElement,
            scope,
            form::evaluateExpressions,
            context.cacheDir.absolutePath
        )
    ) {
        AttachmentElementState(
            formElement = attachmentFormElement,
            scope = scope,
            id = attachmentFormElement.hashCode(),
            evaluateExpressions = form::evaluateExpressions,
            filesDir = context.cacheDir.absolutePath
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
