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


import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.mapping.featureforms.FeatureForm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * A FormElement type representing an Attachment.
 */
internal class AttachmentFormElement(
    feature: ArcGISFeature,
) {
    val label: String = "Attach Photo"

    val description: String = "some description"

    private val _isVisible = MutableStateFlow<Boolean>(false)
    val isVisible = _isVisible.asStateFlow()

    /**
     * The input user interface to use for the element.
     */
    val input: AttachmentFormInput = AttachmentFormInput.AnyAttachmentFormInput

    /**
     * True if the element is editable. False if the element is not editable.
     */
    val isEditable: Boolean = feature.canEditAttachments


    companion object {
        fun Saver(form: FeatureForm) = listSaver(
            save = {
                listOf("")
            },
            restore = {
                AttachmentFormElement(form.feature)
            }
        )
    }
}

@Composable
internal fun rememberAttachmentElement(
    form: FeatureForm
): AttachmentFormElement? {
    if (!form.feature.canEditAttachments) return null
    return rememberSaveable(
        inputs = arrayOf(form),
        saver = AttachmentFormElement.Saver(form)
    ) {
        AttachmentFormElement(form.feature)
    }
}
/**
 * Represents an attachment belonging to a feature form. Wraps the Attachment object and adds additional
 * properties and methods to support displaying attachments in a feature form.
 *
 * The [FormAttachment] must be loaded before calling [createFullImage] or [createThumbnail].
 */
public class FormAttachment(
    private val attachment: Attachment,
    private val filesDir: String
) : Loadable {
    public val contentType: String = attachment.contentType

    public var filePath: String = ""
        private set

    public var isLocal: Boolean = false
        private set

    public var name: String = attachment.name.substringBeforeLast(".")
        private set

    public val size: Int = attachment.size

    private val _loadStatus: MutableStateFlow<LoadStatus> = MutableStateFlow(LoadStatus.NotLoaded)
    override val loadStatus: StateFlow<LoadStatus> = _loadStatus.asStateFlow()

    @Suppress("DEPRECATION")
    public suspend fun createFullImage(): Result<BitmapDrawable> = withContext(Dispatchers.IO) {
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
    public suspend fun createThumbnail(width: Int, height: Int): Result<BitmapDrawable> =
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

    public fun setName(name: String) {
        this.name = name
    }

    override fun cancelLoad() {
        /** does nothing in this mock api **/
    }

    override suspend fun load(): Result<Unit> {
        if (_loadStatus.value !is LoadStatus.Loaded) {
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
        }
        return Result.success(Unit)
    }

    override suspend fun retryLoad(): Result<Unit> {
        return load()
    }
    
    internal companion object {
        fun Saver(attachment: Attachment, filesDir: String): Saver<FormAttachment, Any> = listSaver(
            save = {
                listOf(
                    it.filePath,
                    it.isLocal,
                    it._loadStatus.value.isTerminal
                )
            },
            restore = {
                FormAttachment(attachment, filesDir).apply {
                    filePath = it[0] as String
                    isLocal = it[1] as Boolean
                    (it[2] as Boolean).let { terminal ->
                        if (terminal && filePath.isEmpty()) {
                            _loadStatus.value = LoadStatus.FailedToLoad(IllegalStateException("TODO: use the real API"))
                        } else if (terminal) {
                            _loadStatus.value = LoadStatus.Loaded
                        } else {
                            _loadStatus.value = LoadStatus.NotLoaded
                        }
                    }
                }
            }
        )
    }
}

@Composable
internal fun rememberFormAttachment(attachment: Attachment, filesDir: String)  = rememberSaveable(
inputs = arrayOf(attachment),
saver = FormAttachment.Saver(attachment, filesDir)
) {
    FormAttachment(attachment, filesDir)
}

internal sealed class AttachmentFormInput {
    data object AnyAttachmentFormInput : AttachmentFormInput()
}
