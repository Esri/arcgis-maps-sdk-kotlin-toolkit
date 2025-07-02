/*
 *
 *  Copyright 2025 Esri
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

/**
 * Represents the metadata for an area of an offline map.
 *
 * @since 200.8.0
 */
@Serializable
internal data class OfflineMapAreaMetadata(
    val itemId: String,
    val title: String,
    val description: String,
    @Transient val thumbnailImage: Bitmap? = null
) {

    companion object {
        /**
         * Creates an [OfflineMapAreaMetadata] from a [PreplannedMapArea].
         *
         * @since 200.8.0
         */
        internal fun createPreplannedMetadata(preplannedMapArea: PreplannedMapArea): OfflineMapAreaMetadata {
            return OfflineMapAreaMetadata(
                itemId = preplannedMapArea.portalItem.itemId,
                title = preplannedMapArea.portalItem.title,
                thumbnailImage = preplannedMapArea.portalItem.thumbnail?.image?.bitmap,
                description = preplannedMapArea.portalItem.description
            )
        }

        /**
         * Creates an [OfflineMapAreaMetadata] from an [OnDemandMapAreaConfiguration].
         *
         * @since 200.8.0
         */
        internal fun createOnDemandMetadata(onDemandMapAreaConfiguration: OnDemandMapAreaConfiguration): OfflineMapAreaMetadata {
            return OfflineMapAreaMetadata(
                itemId = onDemandMapAreaConfiguration.itemId,
                title = onDemandMapAreaConfiguration.title,
                thumbnailImage = onDemandMapAreaConfiguration.thumbnail,
                description = ""
            )
        }

        /**
         * Creates an [OfflineMapAreaMetadata] from a [directory] on disk, if “metadata.json” exists.
         *
         * @since 200.8.0
         */
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
                itemId = baseMetadata.itemId,
                thumbnailImage = thumbnail,
                title = baseMetadata.title,
                description = baseMetadata.description
            )
        }

        /**
         * Returns true if metadata.json is found in the [directory].
         */
        internal fun isSerializedFilePresent(directory: File): Boolean {
            return File(directory, offlineAreaMetadataJsonFile).exists()
        }
    }

    /**
     * Save this OfflineMapAreaMetadata into a [directory] on disk.
     *
     * @since 200.8.0
     */
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
