package com.arcgismaps.toolkit.offline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreaConfiguration
import com.arcgismaps.toolkit.offline.workmanager.offlineAreaMetadataJsonFile
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoThumbnailFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

@Serializable
internal data class OfflineMapAreaMetadata(
    val areaId: String,
    @Transient val thumbnailImage: Bitmap? = null,
    val title: String,
    val description: String
) {

    companion object {
        internal fun createPreplannedMetadata(preplannedMapArea: PreplannedMapArea): OfflineMapAreaMetadata {
            return OfflineMapAreaMetadata(
                areaId = preplannedMapArea.portalItem.itemId,
                title = preplannedMapArea.portalItem.title,
                thumbnailImage = preplannedMapArea.portalItem.thumbnail?.image?.bitmap,
                description = preplannedMapArea.portalItem.description
            )
        }

        internal fun createOnDemandMetadata(onDemandMapAreaConfiguration: OnDemandMapAreaConfiguration): OfflineMapAreaMetadata {
            return OfflineMapAreaMetadata(
                areaId = onDemandMapAreaConfiguration.areaID,
                title = onDemandMapAreaConfiguration.title,
                thumbnailImage = onDemandMapAreaConfiguration.thumbnail,
                description = ""
            )
        }

        internal fun createFromDirectory(directory: File): OfflineMapAreaMetadata? {
            val metadataFile = File(directory, offlineAreaMetadataJsonFile)
            if (!metadataFile.exists()) {
                return null
            }
            val jsonString = runCatching { metadataFile.readText(Charsets.UTF_8) }.getOrNull()
                ?: return null
            val baseMetadata = runCatching {
                Json.decodeFromString(serializer(), jsonString)
            }.getOrNull() ?: return null
            val thumbnailFile = File(directory, offlineMapInfoThumbnailFile)
            val thumbnail: Bitmap? = if (thumbnailFile.exists()) {
                BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            } else {
                null
            }
            return OfflineMapAreaMetadata(
                areaId = baseMetadata.areaId,
                thumbnailImage = thumbnail,
                title = baseMetadata.title,
                description = baseMetadata.description
            )
        }

        internal fun isSerializedFilePresent(directory: File): Boolean {
            return File(directory, offlineAreaMetadataJsonFile).exists()
        }
    }

    internal fun saveToDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val metadataFile = File(directory, offlineAreaMetadataJsonFile)
        val jsonString = Json.encodeToString(serializer(), this)
        runCatching { metadataFile.writeText(jsonString, Charsets.UTF_8) }
        thumbnailImage?.let { bmp ->
            val thumbFile = File(directory, offlineMapInfoThumbnailFile)
            runCatching {
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }
}
