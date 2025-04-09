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
import android.graphics.Bitmap
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
     * Backing mutable state for the [attachments] property.
     */
    private val _attachments = mutableStateListOf<FormAttachmentState>()

    /**
     * The attachments associated with the form element. This list is observable and will update
     * the UI when attachments are added or removed.
     */
    val attachments : List<FormAttachmentState>
        get() = _attachments

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
            formElement.fetchAttachments().onSuccess {
                // build a state list of attachments
                buildAttachmentStates(formElement.attachments)
            }
        }
    }

    /**
     *  Loads the attachments provided in the [list] and transforms them into state objects
     *  to produce the [attachments] list.
     */
    private fun buildAttachmentStates(list: List<FormAttachment>) {
        _attachments.clear()
        list.asReversed().forEach { formAttachment ->
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
            if (formAttachment.loadStatus.value is LoadStatus.Loaded || formAttachment.isLocal) {
                state.loadWithParentScope()
            }
            _attachments.add(state)
        }
    }

    /**
     * Adds an attachment with the given [name], [contentType], and [data].
     */
    fun addAttachment(name: String, contentType: String, data: ByteArray): Result<Unit> {
        val formAttachment = formElement.addAttachmentOrNull(name, contentType, data)
            ?: return Result.failure(Exception("Failed to add attachment"))
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
        // add the new state to the beginning of the list and scroll to the new attachment in
        // one atomic operation
        Snapshot.withMutableSnapshot {
            _attachments.add(0, state)
            lazyListState.requestScrollToItem(0)
        }
        // load the new attachment
        state.loadWithParentScope()
        // scroll to the new attachment after a delay to allow the recomposition to complete
        scope.launch {
            evaluateExpressions()
        }
        return Result.success(Unit)
    }

    /**
     * Deletes the given [formAttachment].
     */
    fun deleteAttachment(formAttachment: FormAttachment) {
        formElement.deleteAttachment(formAttachment)
        // delete the state object
        _attachments.removeIf { state ->
            state.formAttachment == formAttachment
        }
        scope.launch { evaluateExpressions() }
    }

    /**
     * Renames the given [formAttachment] with the new [newName].
     */
    fun renameAttachment(formAttachment: FormAttachment, newName: String) {
        // update the state object which also updates FormAttachment
        attachments.firstOrNull { state ->
            state.formAttachment == formAttachment
        }?.name = newName
        scope.launch { evaluateExpressions() }
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
    val deleteAttachment: () -> Unit,
    private val filesDir: String,
    private val scope: CoroutineScope,
    val formAttachment: FormAttachment? = null
) : Loadable {

    /**
     * Backing mutable state for the [name] property.
     */
    private var _name: MutableState<String> = mutableStateOf(name)

    /**
     * The name of the attachment. Setting the name will update the [FormAttachment.name] property.
     * This is backed by a [MutableState] and can be observed by the composition.
     */
    var name: String
        get() = _name.value
        set(value) {
            formAttachment?.name = value
            _name.value = value
        }

    /**
     * A unique ID for the attachment.
     */
    val id: String
        get() = formAttachment?.attachment?.let {
            "${it.id}_${it.name}"
        } ?: name

    private val _loadStatus: MutableStateFlow<LoadStatus> = MutableStateFlow(LoadStatus.NotLoaded)
    override val loadStatus = _loadStatus.asStateFlow()

    /**
     * The file path of the attachment on disk. This is empty until [load] is called.
     */
    val filePath: String
        get() = formAttachment?.filePath ?: ""

    private var _thumbnail: MutableState<Bitmap?> = mutableStateOf(null)

    /**
     * The thumbnail image. This is null until [load] is called.
     */
    val thumbnail: State<Bitmap?> = _thumbnail

    /**
     * The maximum attachment size in bytes that can be loaded. If [size] is greater than this limit,
     * then the attachment will fail to load with an [AttachmentSizeLimitExceededException] when
     * [load] is called.
     */
    val maxAttachmentSize = 50_000_000L

    /**
     * The size of the thumbnail image.
     */
    private val thumbnailSize = Size(368, 300)

    /**
     * A callback that is invoked when the attachment fails to load.
     */
    private var onLoadErrorCallback : ((Throwable) -> Unit)? = null

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
     * Sets a callback that is invoked when the attachment fails to load. This is useful for
     * handling any errors in the UI.
     */
    fun setOnLoadErrorCallback(callback: ((Throwable) -> Unit)?) {
        onLoadErrorCallback = callback
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
            result = when {
                formAttachment == null -> Result.failure(IllegalStateException("Form attachment is null"))
                formAttachment.size == 0L -> Result.failure(EmptyAttachmentException())
                formAttachment.size > maxAttachmentSize -> Result.failure(AttachmentSizeLimitExceededException(maxAttachmentSize))
                else -> formAttachment.retryLoad().onSuccess {
                    createThumbnail()
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
                onLoadErrorCallback?.invoke(error)
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
     * Creates a thumbnail image for the attachment.
     */
    private suspend fun createThumbnail() = withContext(Dispatchers.IO) {
        if (formAttachment == null) {
            return@withContext
        }
        _thumbnail.value = try {
            when (type) {
                is FormAttachmentType.Image -> {
                    formAttachment.createThumbnail(thumbnailSize.width, thumbnailSize.height)
                        .getOrThrow().bitmap
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
            if (ex is CancellationException) {
                throw ex
            }
            null
        }
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
 * Returns a new attachment name based on the [contentType] and [extension].
 *
 * @param contentType The content type of the attachment.
 * @param extension The file extension of the attachment.
 * @return A new attachment name including the file extension specified by [extension].
 */
internal fun AttachmentElementState.getNewAttachmentNameForContentType(
    contentType: String,
    extension: String
): String {
    // use the content type prefix to generate a new attachment name
    val prefix = contentType.split("/").firstOrNull()?.replaceFirstChar(Char::titlecase)
        ?: "Attachment"
    var count = attachments.count { entry ->
        // count the number of attachments with the same content type
        entry.contentType == contentType
    } + 1
    // create a set of attachment names to check for duplicates
    val names = attachments.mapTo(hashSetOf()) { it.name }
    while (names.contains("${prefix}$count.$extension")) {
        count++
    }
    return "${prefix}$count.$extension"
}

/**
 * Exception indicating that the attachment size exceeds the maximum limit.
 *
 * @param limit The maximum attachment size limit in bytes.
 */
internal class AttachmentSizeLimitExceededException(val limit : Long) : Exception("Attachment size exceeds the maximum limit of $limit MB")

/**
 * Exception indicating that the attachment size is 0.
 */
internal class EmptyAttachmentException : Exception("Attachment size is 0")
