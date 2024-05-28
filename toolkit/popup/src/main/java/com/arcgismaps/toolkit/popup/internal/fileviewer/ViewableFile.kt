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
@Parcelize
internal open class ViewableFile(
    open val name: String,
    open val size: Long,
    open val path: String,
    @TypeParceler<ViewableFileType, ViewableFileTypeParceler>() open val type: ViewableFileType
): Parcelable

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
