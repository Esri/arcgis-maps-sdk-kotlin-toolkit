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
import com.arcgismaps.mapping.PortalItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Represents the information of an offline map.
 *
 * @since 200.8.0
 */
public class OfflineMapInfo internal constructor(
    codableInfo: CodableInfo,
    public val thumbnail: Bitmap?
) {

    /**
     *
     */
    private var info: CodableInfo = codableInfo

    /**
     *
     */
    public val id: String
        get() = info.portalItemID

    /**
     *
     */
    public val title: String
        get() = info.title

    /**
     *
     */
    public val description: String
        get() = info.description

    /**
     *
     */
    public val portalItemUrl: String
        get() = info.portalItemURL


    public companion object {
        /// The file names for the serialized offline map info
        private const val INFO_FILENAME = "info.json"
        private const val THUMBNAIL_FILENAME = "thumbnail.png"

        /**
         * Load a [portalItem], and it's thumbnail (if any), then return an [OfflineMapInfo].
         *
         * @since 200.8.0
         */
        public suspend fun createFromPortalItem(portalItem: PortalItem): OfflineMapInfo {
            runCatching { portalItem.load() }

            val thumbBitmap: Bitmap? = portalItem.thumbnail?.let { loadableImage ->
                runCatching { loadableImage.load() }
                loadableImage.image?.bitmap
            }

            val codable = CodableInfo(
                portalItemID = portalItem.itemId,
                title = portalItem.title,
                description = portalItem.description,
                portalItemURL = portalItem.url
            )
            return OfflineMapInfo(codable, thumbBitmap)
        }

        /**
         * Load an [OfflineMapInfo] from a [directory] on disk (if “info.json” exists there).
         *
         * @return [OfflineMapInfo] or null if no info.json or parse failed
         * @since 200.8.0
         */
        public fun makeFromDirectory(directory: File): OfflineMapInfo? {
            val infoFile = File(directory, INFO_FILENAME)
            if (!infoFile.exists()) {
                return null
            }

            val jsonString =
                runCatching { infoFile.readText(Charsets.UTF_8) }.getOrNull() ?: return null
            val codable = runCatching {
                Json.decodeFromString(
                    CodableInfo.serializer(),
                    jsonString
                )
            }.getOrNull() ?: return null

            val thumbnailFile = File(directory, THUMBNAIL_FILENAME)
            val bmp: Bitmap? = if (thumbnailFile.exists()) {
                BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            } else {
                null
            }

            return OfflineMapInfo(codable, bmp)
        }

        /**
         * Delete any saved “info.json” and “thumbnail.png” from this [directory].
         */
        public fun removeFromDirectory(directory: File) {
            File(directory, INFO_FILENAME).delete()
            File(directory, THUMBNAIL_FILENAME).delete()
        }

        /**
         * Returns true if “info.json” exists in the given [directory].
         */
        public fun doesInfoExist(directory: File): Boolean {
            return File(directory, INFO_FILENAME).exists()
        }
    }

    /**
     * Save this OfflineMapInfo into a [directory] on disk.
     *
     * @since 200.8.0
     */
    public fun saveToDirectory(directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val infoFile = File(directory, INFO_FILENAME)
        val jsonString = Json.encodeToString(CodableInfo.serializer(), info)
        runCatching {
            infoFile.writeText(jsonString, Charsets.UTF_8)
        }

        thumbnail?.let { bmp ->
            val thumbFile = File(directory, THUMBNAIL_FILENAME)
            runCatching {
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    /**
     * The codable info is stored in json.
     */
    @Serializable
    internal data class CodableInfo(
        val portalItemID: String,
        val title: String,
        val description: String,
        val portalItemURL: String
    )
}
