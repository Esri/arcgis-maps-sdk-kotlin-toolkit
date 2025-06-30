package com.arcgismaps.toolkit.offline

import android.graphics.Bitmap
import androidx.annotation.StringRes

internal data class OfflineMapAreaMetadata(
    val title: String,
    val thumbnailImage: Bitmap?,
    val description: String,
    val isDownloaded: Boolean,
    val allowsDownload: Boolean,
    val directorySize: Int,
    val dismissMetadataViewOnDelete: Boolean,
    @StringRes val removeDownloadButtonTextResId: Int
){

}