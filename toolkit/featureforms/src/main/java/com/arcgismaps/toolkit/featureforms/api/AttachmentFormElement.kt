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

package com.arcgismaps.toolkit.featureforms.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import android.content.Context

/**
 * A FormElement type representing an Attachment. Use the factory method [createOrNull] to create
 * an instance.
 */
internal class AttachmentFormElement private constructor(
    private val feature: ArcGISFeature,
    private val filesDir: String
) {
    /**
     * The input user interface to use for the element.
     */
    val input: AttachmentFormInput = AttachmentFormInput.AnyAttachmentFormInput

    /**
     * True if the element is editable. False if the element is not editable.
     */
    val isEditable: Boolean = feature.canEditAttachments

    private val _attachments: MutableList<FormAttachment> = mutableListOf()

    /**
     * Returns all the current attachments.
     */
    val attachments: List<FormAttachment> = _attachments

    private val mutex = Mutex()

    /**
     * Adds the specified [data] as an attachment with the specified [name] and content [contentType].
     *  This method is thread-safe.
     */
    suspend fun addAttachment(
        name: String,
        contentType: String,
        data: ByteArray
    ): Result<FormAttachment> {
        mutex.withLock {
            val attachment = feature.addAttachment(name, contentType, data).getOrNull()
            return if (attachment != null) {
                val formAttachment = FormAttachment(attachment, filesDir)
                formAttachment.load()
                _attachments.add(formAttachment)
                Result.success(formAttachment)
            } else {
                Result.failure(Exception("Unable to create attachment"))
            }
        }
    }

    /**
     * Loads and adds the specified [uri] as a [File] as an attachment with the specified [name].
     */
    suspend fun addAttachment(
        name: String,
        contentType: String,
        uri: String
    ): Result<FormAttachment> {
        try {
            var bytes: ByteArray?
            FileInputStream(File((uri))).use {
                bytes = it.readBytes()
            }
            bytes?.let {
                return addAttachment(name, contentType, it)
            }
            return Result.failure(Exception("Unable to read from $uri"))
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            return Result.failure(ex)
        }
    }

    /**
     * Adds the specified [bitmapDrawable] as an attachment with the specified [name].
     */
    suspend fun addAttachment(
        name: String,
        contentType: String,
        bitmapDrawable: BitmapDrawable
    ): Result<FormAttachment> {
        val byteArray = bitmapDrawable.bitmap.toByteArray()
        return addAttachment(name, contentType, byteArray)
    }

    /**
     * Deletes the specified [attachment]. This method is thread-safe.
     */
    suspend fun deleteAttachment(attachment: FormAttachment): Result<Unit> {
        mutex.withLock {
            if (_attachments.contains(attachment)) {
                feature.deleteAttachment(attachment.attachment).onFailure {
                    return Result.failure(it)
                }
                _attachments.remove(attachment)
                return Result.success(Unit)
            } else {
                return Result.failure(NoSuchElementException())
            }
        }
    }

    companion object {

        /**
         * Creates a new [AttachmentFormElement] from the give [feature].
         *
         * @param feature The [ArcGISFeature] to create the [AttachmentFormElement] from.
         * @param filesDir The directory to the cache any attachments. Use [Context.getFilesDir].
         *
         * @return Returns null if unable to create a [AttachmentFormElement].
         */
        suspend fun createOrNull(feature: ArcGISFeature, filesDir: String): AttachmentFormElement? {
            feature.load().onFailure { return null }
            val featureAttachments = feature.fetchAttachments().onFailure {
                return null
            }.getOrNull()!!
            val formAttachments = featureAttachments.map {
                FormAttachment(it, filesDir)
            }
            return AttachmentFormElement(feature, filesDir).apply {
                _attachments.addAll(formAttachments)
            }
        }
    }
}

/**
 * Represents an attachment belonging to a feature form. Wraps the Attachment object and adds additional
 * properties and methods to support displaying attachments in a feature form.
 *
 * The [FormAttachment] must be loaded before calling [createFullImage] or [createThumbnail].
 */
internal class FormAttachment(
    val attachment: Attachment,
    private val filesDir: String
) : Loadable {
    val contentType: String = attachment.contentType

    var filePath: String = ""
        private set

    var isLocal: Boolean = false
        private set

    val name: String = attachment.name

    val size: Int = attachment.size

    private val _loadStatus: MutableStateFlow<LoadStatus> = MutableStateFlow(LoadStatus.NotLoaded)
    override val loadStatus: StateFlow<LoadStatus> = _loadStatus.asStateFlow()

    @Suppress("DEPRECATION")
    suspend fun createFullImage(): Result<BitmapDrawable> = withContext(Dispatchers.IO) {
        return@withContext if (filePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap != null) {
                Result.success(BitmapDrawable(bitmap))
            } else {
                Result.failure(Exception("Unable to create an image"))
            }
        } else {
            Result.failure(Exception("Attachment is not loaded"))
        }
    }

    @Suppress("DEPRECATION")
    suspend fun createThumbnail(width: Int, height: Int): Result<BitmapDrawable> =
        withContext(Dispatchers.IO) {
            return@withContext if (filePath.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeFile(filePath)
                if (bitmap != null) {
                    val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height)
                    Result.success(BitmapDrawable(thumbnail))
                } else {
                    Result.failure(Exception("Unable to create an image"))
                }
            } else {
                Result.failure(Exception("Attachment is not loaded"))
            }
        }

    override fun cancelLoad() {
        /** does nothing in this mock api **/
    }

    override suspend fun load(): Result<Unit> {
        _loadStatus.value = LoadStatus.Loading
        try {
            if (!attachment.hasFetchedData || filePath.isEmpty()) {
                val data = attachment.fetchData().getOrThrow()
                val dir = File("$filesDir/attachments")
                dir.mkdirs()
                val file = File(dir, attachment.name)
                file.createNewFile()
                FileOutputStream(file).use {
                    it.write(data)
                }
                filePath = file.absolutePath
                isLocal = true
            }
        } catch (ex: Exception) {
            _loadStatus.value = LoadStatus.FailedToLoad(ex)
            if (ex is CancellationException) {
                throw ex
            }
            return Result.failure(ex)
        }
        _loadStatus.value = LoadStatus.Loaded
        return Result.success(Unit)
    }

    override suspend fun retryLoad(): Result<Unit> {
        return load()
    }
}

internal sealed class AttachmentFormInput {
    data object AnyAttachmentFormInput : AttachmentFormInput()
}

internal fun Bitmap.toByteArray(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(allocationByteCount)
    copyPixelsToBuffer(byteBuffer)
    return byteBuffer.array()
}
