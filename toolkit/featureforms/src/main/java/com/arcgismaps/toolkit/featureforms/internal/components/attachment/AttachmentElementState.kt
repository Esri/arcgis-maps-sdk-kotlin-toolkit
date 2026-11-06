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

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import android.webkit.MimeTypeMap
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.internal.utils.runCatchingCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.Objects
import java.util.UUID

/**
 * The maximum attachment size in bytes that can be added.
 */
internal const val maxAttachmentUploadSize = 999_000_000L

/**
 * The maximum attachment size in bytes that can be downloaded.
 */
internal const val maxAttachmentsDownloadSize = 999_000_000L

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
    private val evaluateExpressions: suspend () -> Unit
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
    val attachments: List<FormAttachmentState>
        get() = _attachments

    /**
     * Indicates whether renaming an attachment is allowed.
     */
    val allowUserRename = formElement.allowUserRename

    /**
     * Indicates whether the attachment form element is editable.
     */
    val isEditable = formElement.isEditable

    /**
     * Indicates whether the filename of the attachment should be displayed.
     */
    val displayFilename = formElement.displayFilename

    /**
     * The input type for the attachment form element. This is determined based on the allowed
     * attachment types specified by the form element.
     */
    val inputs = formElement.inputs

    /**
     * The attachment keyword association for the attachment form element.
     */
    val attachmentKeywordAssociation = formElement.attachmentKeywordAssociation

    /**
     * The state of the lazy list that displays the [attachments].
     */
    val lazyListState = LazyListState()

    /**
     * The maximum number of attachments that can be added.
     */
    val maxAttachmentCount = formElement.maxAttachmentCount

    /**
     * The minimum number of attachments that must be added.
     */
    val minAttachmentCount = formElement.minAttachmentCount

    /**
     * Indicates whether to use the original filename of the attachment when adding an attachment.
     */
    val useOriginalFilename = formElement.useOriginalFilename

    /**
     * A validation error for the attachment form element.
     */
    val validationError: ValidationErrorState
        get() = _validationError.value

    /**
     * Backing mutable state for the [validationError] property.
     */
    private var _validationError: MutableState<ValidationErrorState> = mutableStateOf(
        ValidationErrorState.NoError
    )

    /**
     * Indicates whether the attachment form element has ever been focused.
     */
    val wasFocused: Boolean
        get() = _wasFocused.value

    /**
     * Backing mutable state for the [wasFocused] property.
     */
    private var _wasFocused = mutableStateOf(false)

    /**
     * A list of validation errors for the attachments.
     */
    private val validationErrors: StateFlow<List<ValidationErrorState>> =
        formElement.mapValidationErrors(scope)

    init {
        scope.launch {
            // Produce a validation error based on the current state of the errors and the focused
            // state. The error is only shown when the element is focused.
            combine(
                snapshotFlow { _wasFocused.value },
                validationErrors
            ) { focused, errors ->
                Pair(focused, errors)
            }.collect {
                val (focused, errors) = it
                _validationError.value = if (focused) {
                    errors.firstOrNull() ?: ValidationErrorState.NoError
                } else {
                    ValidationErrorState.NoError
                }
            }
        }
        refreshAttachments()
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

    fun refreshAttachments() {
        scope.launch {
            formElement.fetchAttachments().onSuccess {
                // build a state list of attachments
                buildAttachmentStates(formElement.attachments)
            }
        }
    }

    /**
     * Adds an attachment with the given [name], [contentType], and [filePath]. If the [source] is
     * [AttachmentSource.FileSystem], the attachment will be added with the given [name]. If the
     * [source] is [AttachmentSource.Capture], the attachment is added with the API generated name.
     *
     * @param name The name of the attachment.
     * @param contentType The content type of the attachment.
     * @param filePath The file path of the attachment.
     * @param source The source of the attachment.
     */
    suspend fun addAttachment(
        name: String,
        contentType: String,
        filePath: String,
        source: AttachmentSource
    ): Result<Unit> {
        return when (source) {
            // If the attachment is from the camera, use the API generated name.
            AttachmentSource.Capture -> formElement.addAttachment(
                contentType = contentType,
                filePath = filePath
            )
            // If the attachment is from the file system, use the provided name.
            AttachmentSource.FileSystem -> formElement.addAttachment(
                name = name,
                contentType = contentType,
                filePath = filePath
            )
        }.onSuccess { formAttachment ->
            // create a new state
            val attachment = FormAttachmentState(
                name = formAttachment.name,
                size = formAttachment.size,
                contentType = formAttachment.contentType,
                type = formAttachment.type,
                elementStateId = id,
                deleteAttachment = { deleteAttachment(formAttachment) },
                scope = scope,
                formAttachment = formAttachment
            )
            // add the new state to the beginning of the list and scroll to the new attachment in
            // one atomic operation
            Snapshot.withMutableSnapshot {
                _attachments.add(0, attachment)
                lazyListState.requestScrollToItem(0)
            }
            // load the new attachment
            attachment.loadWithParentScope()
            // evaluate expressions after the attachment is added
            scope.launch { evaluateExpressions() }
        }.map {}
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
     * Changes the current focus state for the element.
     */
    fun onFocusChanged(focus: Boolean) {
        if (focus) {
            _wasFocused.value = true
        }
    }

    fun forceValidation() {
        _wasFocused.value = true
    }

    /**
     * Checks if the provided permission is granted.
     */
    fun hasPermission(context: Context, permission: String): Boolean = ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
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
     * The size of the thumbnail image.
     */
    private val thumbnailSize = Size(368, 300)

    /**
     * A callback that is invoked when the attachment fails to load.
     */
    private var onLoadErrorCallback: ((Throwable) -> Unit)? = null

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
    override suspend fun load(): Result<Unit> = withContext(Dispatchers.IO) {
        _loadStatus.value = LoadStatus.Loading
        var result = Result.success(Unit)
        try {
            result = when {
                formAttachment == null -> Result.failure(IllegalStateException("Form attachment is null"))
                formAttachment.size == 0L -> Result.failure(EmptyAttachmentException())
                formAttachment.size > maxAttachmentsDownloadSize -> Result.failure(
                    AttachmentSizeLimitExceededException(maxAttachmentsDownloadSize)
                )

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
        return@withContext result
    }

    override fun cancelLoad() {
        formAttachment?.cancelLoad()
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
    @Suppress("DEPRECATION")
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
 * Returns a random temporary attachment name.
 *
 * @param extension The file extension of the attachment.
 * @return A new attachment name including the file extension specified by [extension].
 */
internal fun generateTemporaryAttachmentName(
    extension: String
): String = "Attachment-${UUID.randomUUID()}.$extension"

/**
 * Adds an attachment to the [AttachmentElementState] from the specified [File]. The file will be
 * deleted after the attempt to add the attachment. Hence, ensure the file is not needed or is a
 * temporary file before calling this method.
 *
 * @param file The file to add as an attachment.
 * @param source The source of the attachment.
 * @return A [Result] indicating success or failure.
 */
internal suspend fun AttachmentElementState.addAttachmentFromFile(
    file: File,
    source: AttachmentSource
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        if (file.exists().not()) {
            return@withContext Result.failure(
                FileNotFoundException("File not found: ${file.absolutePath}")
            )
        }
        // get the name and content type of the file
        val name = file.name
        val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            file.extension.lowercase()
        ) ?: "application/octet-stream"
        // validate the file size before adding the attachment
        return@withContext when {
            file.length() == 0L -> Result.failure(EmptyAttachmentException())
            file.length() > maxAttachmentUploadSize -> Result.failure(
                AttachmentSizeLimitExceededException(maxAttachmentUploadSize)
            )

            else -> addAttachment(name, contentType, file.absolutePath, source)
        }
    } finally {
        file.deleteIfExists()
    }
}

/**
 * Adds an attachment to the [AttachmentElementState] from the specified [uri]. The source of the
 * provided [uri] must always be [AttachmentSource.FileSystem]. This is typically used when the
 * uri is obtained from external or scoped storage sources.
 *
 * @param uri The uri of the attachment.
 * @param context The context.
 * @return A [Result] indicating success or failure.
 */
internal suspend fun AttachmentElementState.addAttachmentFromUri(
    uri: Uri,
    context: Context
): Result<Unit> = runCatchingCancellable {
    withContext(Dispatchers.IO) {
        // get the content type of the uri
        val contentType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"

        // get the file extension from the content type
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
            ?: throw Exception(context.getString(R.string.attachment_error))

        // generate an initial name for the attachment
        var name = generateTemporaryAttachmentName(extension)
        // size of the attachment
        var size = 0L
        // get the name and size of the attachment
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                // use the default file name from the uri if available
                cursor.getStringOrNull(nameIndex)?.let {
                    name = it
                }
                // update the size
                cursor.getLongOrNull(sizeIndex)?.let {
                    size = it
                }
            }
        }

        // Add the attachment if it passes all the checks
        return@withContext when {
            size == 0L -> throw EmptyAttachmentException()
            size > maxAttachmentUploadSize -> throw AttachmentSizeLimitExceededException(
                maxAttachmentUploadSize
            )

            else -> {
                // Cache the file from the URI and get the cached file reference. This is required to get a file
                // path that can be accessed by the toolkit/app.
                context.cacheFile(uri, extension).fold(
                    onSuccess = { cachedFile ->
                        try {
                            // Note here the name is from the original uri.
                            addAttachment(
                                name = name,
                                contentType = contentType,
                                filePath = cachedFile.absolutePath,
                                source = AttachmentSource.FileSystem
                            ).getOrThrow()
                        } finally {
                            // delete the cached file after attempting to add the attachment since it's
                            // no longer needed
                            cachedFile.deleteIfExists()
                        }
                    },
                    onFailure = { ex -> throw ex }
                )
            }
        }
    }
}


/**
 * Copies the content from the specified [uri] to a file in the cache directory given by
 * [Context.getCacheDir]. A unique file name is generated for the cached file to avoid conflicts
 * with other files in the directory.
 *
 * @param uri The uri of the file to copy.
 * @return The file in the cache directory that contains the copied content.
 */
internal suspend fun Context.cacheFile(
    uri: Uri,
    extension: String
): Result<File> = runCatchingCancellable {
    require(extension.isNotEmpty()) { "File extension cannot be empty" }
    val fileName = generateTemporaryAttachmentName(extension)
    val outFile = cacheDir.resolve(fileName)
    return@runCatchingCancellable try {
        withContext(Dispatchers.IO) {
            // Copy the content from the uri to a file in the cache directory that the toolkit/app
            // controls, so that it can be accessed.
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("Unable to open input stream for URI: $uri")
            inputStream.use { inputStream ->
                outFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            outFile
        }
    } catch (ex: Exception) {
        // If an exception occurs during the copy process, delete the output file if it was created
        outFile.deleteIfExists()
        throw ex
    }
}

/**
 * Deletes the file if it exists. If the file cannot be deleted, a warning is logged. This function
 * is executed in a non-cancellable context to ensure that the file deletion is attempted even if the
 * calling coroutine is canceled.
 */
internal suspend fun File.deleteIfExists() = withContext(NonCancellable + Dispatchers.IO) {
    try {
        if (exists() && delete().not()) {
            Log.w(
                "FeatureFormToolkit",
                "Unable to delete temporary file: $absolutePath"
            )
        }
    } catch (ex: Exception) {
        Log.w(
            "FeatureFormToolkit",
            "Exception occurred while trying to delete temporary file: $absolutePath",
            ex
        )
    }
}

/**
 * Exception indicating that the attachment size exceeds the limit.
 *
 * @param limit The attachment size limit in bytes.
 */
internal class AttachmentSizeLimitExceededException(val limit: Long) : Exception(
    "Attachment size exceeds the limit of ${limit / 1_000_000} MB"
)

/**
 * Exception indicating that the attachment size is 0.
 */
internal class EmptyAttachmentException : Exception("Attachment size is 0")

/**
 * Enum representing the source of an attachment.
 */
internal enum class AttachmentSource {
    Capture,
    FileSystem
}
