/*
 * Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.arcgismaps.toolkit.popup.internal.ui.fileviewer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.arcgismaps.mapping.popup.PopupAttachmentType
import com.arcgismaps.toolkit.popup.R
import com.arcgismaps.toolkit.popup.internal.element.attachment.PopupAttachmentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * A file that can be viewed in the [FileViewer].
 */
@Parcelize
internal data class ViewableFile(
    val name: String,
    val size: Long,
    val path: String,
    @TypeParceler<ViewableFileType, ViewableFileTypeParceler>() val type: ViewableFileType,
    val contentType: String = "image/jpeg"
) : Parcelable

private object ViewableFileTypeParceler : Parceler<ViewableFileType> {
    override fun create(parcel: Parcel): ViewableFileType {
        return when (parcel.readInt()) {
            0 -> ViewableFileType.Image
            1 -> ViewableFileType.Video
            2 -> ViewableFileType.Audio
            else -> ViewableFileType.Other
        }
    }

    override fun ViewableFileType.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(
            when (this) {
                ViewableFileType.Image -> 0
                ViewableFileType.Video -> 1
                ViewableFileType.Audio -> 2
                ViewableFileType.Other -> 3
            }
        )
    }
}

internal sealed class ViewableFileType {
    data object Image : ViewableFileType()
    data object Video : ViewableFileType()
    data object Audio : ViewableFileType()
    data object Other : ViewableFileType()
}

internal fun PopupAttachmentState.getViewableFileType(): ViewableFileType = when (this.popupAttachmentType) {
    PopupAttachmentType.Image -> ViewableFileType.Image
    PopupAttachmentType.Video -> ViewableFileType.Video
    PopupAttachmentType.Document -> ViewableFileType.Other
    PopupAttachmentType.Other ->
        if (this.contentType.lowercase().contains("audio")) ViewableFileType.Audio else ViewableFileType.Other

}

/**
 * Saves the file to the device.
 */
internal suspend fun ViewableFile.saveToDevice(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val sourceFile = File(path).takeIf { it.exists() }
            ?: throw FileNotFoundException("File not found: $path")

        // define the file values
        val fileValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, contentType)
        }

        @SuppressLint("InlinedApi")
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        else
            MediaStore.VOLUME_EXTERNAL

        val contentCollection = when (type) {
            ViewableFileType.Video -> MediaStore.Video.Media.getContentUri(uri)
            ViewableFileType.Image -> MediaStore.Images.Media.getContentUri(uri)
            ViewableFileType.Audio -> MediaStore.Audio.Media.getContentUri(uri)
            else -> throw UnsupportedOperationException("Cannot save this file type")
        }
        val destinationUri = context.contentResolver.insert(contentCollection, fileValues)
            ?: throw IOException("Failed to save file")

        // copy file to destination
        val sourceUri = Uri.fromFile(sourceFile)
        context.contentResolver?.openInputStream(sourceUri)?.use { inputStream ->
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        } ?: throw IOException("Failed to save file")
    }
}

/**
 * Shares the file using Android's share sheet.
 */
internal suspend fun ViewableFile.share(context: Context) = withContext(Dispatchers.IO) {
    val file = File(path)

    val uri = FileProvider.getUriForFile(
        context.applicationContext,
        "${context.applicationContext.applicationInfo.packageName}.arcgis.popup.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        setDataAndType(uri, contentType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share))
    )
}
