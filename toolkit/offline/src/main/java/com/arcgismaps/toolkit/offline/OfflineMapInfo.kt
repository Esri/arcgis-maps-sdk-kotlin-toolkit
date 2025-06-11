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
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoJsonFile
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoThumbnailFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Represents the information of an offline map.
 *
 * @since 200.8.0
 */
public class OfflineMapInfo private constructor(
    private val info: CodableInfo,
    private val _thumbnail: Bitmap?
) {

    /**
     * Load a [portalItem], and it's thumbnail (if any), then construct an [OfflineMapInfo].
     *
     * @since 200.8.0
     */
    internal constructor(portalItem: PortalItem) : this(
        info = runBlocking {
            runCatching { portalItem.load() }
            val itemId = portalItem.itemId
            val itemUrl = portalItem.url
            CodableInfo(
                portalItemID = itemId,
                title = portalItem.title,
                description = portalItem.description,
                portalItemURL = itemUrl
            )
        },
        _thumbnail = runBlocking {
            portalItem.thumbnail?.let { loadableImage ->
                runCatching { loadableImage.load() }
                loadableImage.image?.bitmap
            }
        }
    )

    /**
     * The ID of the portal item associated with the map.
     *
     * @since 200.8.0
     */
    public val id: String
        get() = info.portalItemID

    /**
     * The thumbnail of the portal item associated with the map.
     *
     * @since 200.8.0
     */
    public val thumbnail: Bitmap?
        get() = _thumbnail

    /**
     * The title of the portal item associated with the map.
     *
     * @since 200.8.0
     */
    public val title: String
        get() = info.title

    /**
     * The description of the portal item associated with the map.
     *
     * @since 200.8.0
     */
    public val description: String
        get() = info.description

    /**
     * The URL of the portal item associated with the map.
     *
     * @since 200.8.0
     */
    public val portalItemUrl: String
        get() = info.portalItemURL


    public companion object {

        /**
         * Creates an [OfflineMapInfo] from a [directory] on disk, if “info.json” exists.
         *
         * @since 200.8.0
         */
        public fun createFromDirectory(directory: File): OfflineMapInfo? {
            val infoFile = File(directory, offlineMapInfoJsonFile)
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

            val thumbnailFile = File(directory, offlineMapInfoThumbnailFile)
            val bmp: Bitmap? = if (thumbnailFile.exists()) {
                BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            } else {
                null
            }

            return OfflineMapInfo(codable, bmp)
        }

        /**
         * Delete any saved “info.json” and “thumbnail.png” from this [directory].
         *
         * @since 200.8.0
         */
        public fun removeFromDirectory(directory: File) {
            File(directory, offlineMapInfoJsonFile).delete()
            File(directory, offlineMapInfoThumbnailFile).delete()
        }

        /**
         * Returns true if “info.json” exists in the given [directory].
         *
         * @since 200.8.0
         */
        public fun isSerializedFilePresent(directory: File): Boolean {
            return File(directory, offlineMapInfoJsonFile).exists()
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

        val infoFile = File(directory, offlineMapInfoJsonFile)
        val jsonString = Json.encodeToString(CodableInfo.serializer(), info)
        runCatching {
            infoFile.writeText(jsonString, Charsets.UTF_8)
        }

        _thumbnail?.let { bmp ->
            val thumbFile = File(directory, offlineMapInfoThumbnailFile)
            runCatching {
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    /**
     * The codable info is stored in json.
     *
     * @since 200.8.0
     */
    @Serializable
    internal data class CodableInfo(
        val portalItemID: String,
        val title: String,
        val description: String,
        val portalItemURL: String
    )
}
