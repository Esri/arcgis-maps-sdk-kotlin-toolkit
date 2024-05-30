/*
 *  Copyright 2024 Esri
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
 *
 */

package com.arcgismaps.toolkit.popup.internal.fileviewer

import android.os.Parcel
import android.os.Parcelable
import com.arcgismaps.mapping.popup.PopupAttachmentType
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * A file that can be viewed in the [FileViewer].
 */
internal interface ViewableFile {
    val name: String
    val size: Long
    val path: String
    val type: ViewableFileType
}

@Parcelize
internal data class ViewableFileImpl(
    override val name: String,
    override val size: Long,
    override val path: String,
    @TypeParceler<ViewableFileType, ViewableFileTypeParceler>() override val type: ViewableFileType
): ViewableFile, Parcelable

private object ViewableFileTypeParceler: Parceler<ViewableFileType> {
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
