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
import android.graphics.Bitmap.CompressFormat
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.AnyAttachmentsFormInput
import com.arcgismaps.mapping.featureforms.AttachmentChangeType
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.AttachmentsFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
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
@Stable
internal class AttachmentElementState(
    id: Int,
    private val formElement: AttachmentsFormElement,
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
    var attachments = mutableStateListOf<FormAttachmentState>()

    /**
     * Indicates whether the attachment form element is editable.
     */
    val isEditable = formElement.isEditable

    /**
     * The state of the lazy list that displays the [attachments].
     */
    val lazyListState = LazyListState()

    /**
     * The input type of the attachment form element.
     */
    val input = formElement.input

    init {
        scope.launch {
            formElement.fetchAttachments().onSuccess {
                // build a state list of attachments
                buildAttachmentStates(formElement.attachments)
            }
        }
        scope.launch {
            formElement.attachmentChanged.collect {
                when (it.changeType) {
                    is AttachmentChangeType.Deletion -> {
                        // delete the state object
                        attachments.removeIf { state ->
                            state.formAttachment == it.attachment
                        }
                    }

                    is AttachmentChangeType.Rename -> {
                        // update the state object
                        attachments.firstOrNull { state ->
                            state.formAttachment == it.attachment
                        }?.update(it.attachment)
                    }

                    is AttachmentChangeType.Addition -> {
                        val formAttachment = it.attachment
                        // create a new state
                        val state = FormAttachmentState(
                            name = formAttachment.name,
                            size = formAttachment.size,
                            contentType = formAttachment.contentType,
                            type = formAttachment.type,
                            elementStateId = id,
                            deleteAttachment = { deleteAttachment(formAttachment) },
                            filesDir = filesDir,
                            scope = scope,
                            formAttachment = formAttachment
                        )
                        attachments.add(state)
                        // load the new attachment
                        state.loadWithParentScope()
                        // scroll to the new attachment after a delay to allow the recomposition to complete
                        delay(100)
                        lazyListState.scrollToItem(attachments.count())
                    }
                }
                evaluateExpressions()
            }
        }
    }

    /**
     *  Loads the attachments provided in the [list] and transforms them into state objects
     *  to produce the [attachments] list.
     */
    private suspend fun buildAttachmentStates(list: List<FormAttachment>) {
        attachments.clear()
        list.forEach { formAttachment ->
            // create a new state
            val state = FormAttachmentState(
                name = formAttachment.name,
                size = formAttachment.size,
                contentType = formAttachment.contentType,
                type = formAttachment.type,
                elementStateId = id,
                deleteAttachment = { deleteAttachment(formAttachment) },
                filesDir = filesDir,
                scope = scope,
                formAttachment = formAttachment
            )
            // if the attachment is already loaded then re-load the new state
            // this is useful during a configuration change when the form attachment
            // objects have already been loaded by the state object.
            if (formAttachment.loadStatus.value is LoadStatus.Loaded) {
                state.loadWithParentScope()
            }
            attachments.add(state)
        }
    }

    /**
     * Adds an attachment with the given [name], [contentType], and [data].
     */
    suspend fun addAttachment(name: String, contentType: String, data: ByteArray) {
        formElement.addAttachment(name, contentType, data)
    }

    /**
     * Deletes the given [formAttachment].
     */
    private suspend fun deleteAttachment(formAttachment: FormAttachment) {
        formElement.deleteAttachment(formAttachment)
    }

    /**
     * Renames the given [formAttachment] with the new [newName].
     */
    suspend fun renameAttachment(formAttachment: FormAttachment, newName: String) {
        if (formAttachment.name != newName) {
            formElement.renameAttachment(formAttachment, newName)
        }
    }

    /**
     * Checks if the camera permissions are granted.
     */
    fun hasCameraPermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun Saver(
            attachmentFormElement: AttachmentsFormElement,
            scope: CoroutineScope,
            evaluateExpressions: suspend () -> Unit,
            filesDir: String
        ): Saver<AttachmentElementState, Any> = listSaver(
            save = {
                buildList<Int> {
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
                        if (savedList.count() == 2) {
                            // scroll to the last visible item
                            val firstVisibleItemIndex = savedList[0]
                            val firstVisibleItemScrollOffset = savedList[1]
                            it.lazyListState.scrollToItem(
                                firstVisibleItemIndex,
                                firstVisibleItemScrollOffset
                            )
                        }
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
 * @param type The type of the attachment.
 * @param elementStateId The ID of the [AttachmentElementState] that created this attachment.
 * @param deleteAttachment A function to delete the attachment.
 * @param filesDir The directory where the attachments are stored.
 * @param scope The coroutine scope used to launch coroutines.
 * @param formAttachment The [FormAttachment] that this state represents.
 */
@Stable
internal class FormAttachmentState(
    name: String,
    val size: Long,
    val contentType: String,
    val type: FormAttachmentType,
    val elementStateId: Int,
    val deleteAttachment: suspend () -> Unit,
    private val filesDir: String,
    private val scope: CoroutineScope,
    val formAttachment: FormAttachment? = null
) : Loadable {

    /**
     * The name of the attachment.
     */
    var name by mutableStateOf(name)
        private set

    /**
     * A unique ID for the attachment.
     */
    val id by lazy {
        formAttachment?.attachment?.let {
            "${it.id}_${it.name}"
        } ?: name
    }

    private val _loadStatus: MutableStateFlow<LoadStatus> = MutableStateFlow(LoadStatus.NotLoaded)
    override val loadStatus = _loadStatus.asStateFlow()

    /**
     * The file path of the attachment on disk. This is empty until [load] is called.
     */
    var filePath: String = ""
        private set

    private var _thumbnailUri: MutableState<String> = mutableStateOf("")

    /**
     * The URI of the thumbnail image. This is empty until [load] is called.
     */
    val thumbnailUri: State<String> = _thumbnailUri

    /**
     * The directory where the attachments are stored as defined in the [AttachmentsFileProvider].
     */
    private val attachmentsDir = "feature_forms_attachments"

    /**
     * The size of the thumbnail image.
     */
    private val thumbnailSize = Size(368, 300)

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
     * Updates the attachment properties with the given [formAttachment].
     */
    fun update(formAttachment: FormAttachment) {
        // only name is updated since renameAttachment() is the only update call that can be made
        name = formAttachment.name
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
                        // write the data to disk
                        writeDataToDisk(data)
                        // create the thumbnail
                        createThumbnail()
                    } else {
                        result = Result.failure(Exception("Failed to load attachment data"))
                    }
                }
            }
        } catch (ex: CancellationException) {
            result = Result.failure(ex)
            throw ex
        } catch (ex: Exception) {
            result = Result.failure(ex)
        } finally {
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
        // cancel op not supported
    }

    override suspend fun retryLoad(): Result<Unit> {
        return load()
    }

    override fun hashCode(): Int {
        return Objects.hash(id, name, size, type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormAttachmentState

        if (id != other.id) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (type != other.type) return false

        return true
    }

    /**
     * Writes the attachment data to disk. If the file already exists, it will not be overwritten.
     */
    private suspend fun writeDataToDisk(data: ByteArray) = withContext(Dispatchers.IO) {
        val directory = File(filesDir, attachmentsDir)
        directory.mkdirs()
        // write the data to disk using the attachment id as the file name
        val file = File(directory, id)
        // write to the file only if the file does not exist
        if (file.exists().not()) {
            file.createNewFile()
            FileOutputStream(file).use {
                it.write(data)
            }
        }
        filePath = file.absolutePath
    }

    /**
     * Creates a thumbnail image for the attachment. If the thumbnail already exists, it will not be
     * recreated.
     */
    private suspend fun createThumbnail() = withContext(Dispatchers.IO) {
        if (formAttachment == null) {
            return@withContext
        }
        val directory = File(filesDir, attachmentsDir)
        directory.mkdirs()
        val file = File(directory, "thumb_$id")
        if (file.exists()) {
            _thumbnailUri.value = file.absolutePath
            return@withContext
        }
        val bitmap = try {
            when (type) {
                is FormAttachmentType.Image -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ThumbnailUtils.createImageThumbnail(File(filePath), thumbnailSize, null)
                    } else {
                        ThumbnailUtils.createImageThumbnail(filePath, 1)
                    }
                }

                FormAttachmentType.Video -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ThumbnailUtils.createVideoThumbnail(File(filePath), thumbnailSize, null)
                    } else {
                        ThumbnailUtils.createVideoThumbnail(filePath, 1)
                    }
                }

                else -> null
            }
        } catch (ex: Exception) {
            null
        } ?: return@withContext

        // create and write to the thumbnail file if the bitmap is not null
        file.createNewFile()
        BufferedOutputStream(FileOutputStream(file)).use { bos ->
            bitmap.compress(CompressFormat.JPEG, 85, bos)
        }
        _thumbnailUri.value = file.absolutePath
    }
}

/**
 * Represents valid options for capturing attachments. This is used to determine the types of
 * attachments that can be captured by the UI.
 */
internal sealed class CaptureOptions {

    data object Image : CaptureOptions()
    data object Video : CaptureOptions()
    data object Audio : CaptureOptions()
    data object Document : CaptureOptions()
    data object Signature : CaptureOptions()
    data object Any : CaptureOptions()
    data object Unknown : CaptureOptions()

    /**
     * Returns `true` if the capture options include image capture.
     */
    fun hasImageCapture(): Boolean = this is Any || this is Image

    /**
     * Returns `true` if the capture options include video capture.
     */
    fun hasVideoCapture(): Boolean = this is Any || this is Video

    /**
     * Returns `true` if the capture options include image or video capture.
     */
    fun hasMediaCapture(): Boolean = hasImageCapture() || hasVideoCapture()

    /**
     * Returns `true` if the capture options include document capture.
     */
    fun hasFileCapture(): Boolean = this is Any || this is Document

    /**
     * Returns a list of allowed mime types for this capture option.
     */
    fun getAllowedMimeTypes(): List<String> {
        return when (this) {
            is Any -> listOf("*/*")
            Audio -> listOf("audio/*")
            Document -> listOf("application/*", "text/*")
            Image -> listOf("image/*")
            Signature -> listOf("image/*")
            Video -> listOf("video/*")
            Unknown -> emptyList()
        }
    }

    companion object {

        /**
         * Creates a [CaptureOptions] from the given [AttachmentsFormInput].
         */
        fun create(value: AttachmentsFormInput): CaptureOptions = when (value) {
            is AnyAttachmentsFormInput -> Any
            else -> Unknown
        }
    }
}

@Composable
internal fun rememberAttachmentElementState(
    form: FeatureForm,
    attachmentFormElement: AttachmentsFormElement
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

/**
 * Returns an icon for the attachment type.
 */
@Composable
internal fun FormAttachmentType.getIcon(): ImageVector = when (this) {
    FormAttachmentType.Image -> Icons.Outlined.Image
    FormAttachmentType.Audio -> Icons.Outlined.AudioFile
    FormAttachmentType.Video -> Icons.Outlined.VideoCameraBack
    FormAttachmentType.Document -> Icons.Outlined.FilePresent
    FormAttachmentType.Other -> Icons.Outlined.FileCopy
}

/**
 * Returns a new attachment name based on the content type.
 */
internal fun List<FormAttachmentState>.getNewAttachmentNameForContentType(contentType: String): String {
    val prefix = when {
        contentType.startsWith("image/") -> "Image"
        contentType.startsWith("video/") -> "Video"
        else -> "Attachment"
    }
    val count = this.count { entry ->
        entry.contentType.split("/").firstOrNull()
            .equals(contentType.split("/").firstOrNull(), ignoreCase = true)
    } + 1
    return "$prefix $count"
}
