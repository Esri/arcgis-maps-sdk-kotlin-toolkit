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
package com.arcgismaps.toolkit.popup.internal.fileviewer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.arcgismaps.mapping.popup.PopupAttachmentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.io.File

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
            else -> ViewableFileType.Other
        }
    }

    override fun ViewableFileType.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(
            when (this) {
                ViewableFileType.Image -> 0
                ViewableFileType.Video -> 1
                ViewableFileType.Other -> 2
            }
        )
    }
}

internal sealed class ViewableFileType {
    data object Image : ViewableFileType()
    data object Video : ViewableFileType()
    data object Other : ViewableFileType()
}

internal fun PopupAttachmentType.toViewableFileType(): ViewableFileType = when (this) {
    PopupAttachmentType.Image -> ViewableFileType.Image
    PopupAttachmentType.Video -> ViewableFileType.Video
    PopupAttachmentType.Document,
    PopupAttachmentType.Other -> ViewableFileType.Other
}

internal fun ViewableFile.saveToDevice(context: Context): Boolean {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, contentType)
        }

        @SuppressLint("InlinedApi")
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        else
            MediaStore.VOLUME_EXTERNAL
        val collection = when (type) {
            ViewableFileType.Video -> MediaStore.Video.Media.getContentUri(uri)
            ViewableFileType.Image -> MediaStore.Images.Media.getContentUri(uri)
            else -> throw (UnsupportedOperationException("Cannot save this file type"))
        }
        val itemUri = context.contentResolver.insert(collection, values) ?: return false
        val sourceFile = File(path)
        if (!sourceFile.exists()) {
            return false
        }
        val sourceUri = Uri.fromFile(sourceFile)
        context.contentResolver?.openInputStream(sourceUri)?.use { inputStream ->
            context.contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        }
    } catch (exception: Exception) {
        Log.d("ArcGIS-Maps", "Failed to save ViewableFile: ${exception.message}")
        return false
    }
    return true
}

/**
 * Shares the file using Android's share sheet.
 */
internal fun ViewableFile.share(scope: CoroutineScope, context: Context) {
    scope.launch(Dispatchers.IO) {
        val file = File(path)

        val uri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.applicationContext.applicationInfo.processName}.fileprovider",
            file
        )
        val itemType = context.contentResolver.getType(uri)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, itemType)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share")
        )
    }
}
