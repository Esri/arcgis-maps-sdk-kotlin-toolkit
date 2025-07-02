/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offline.workmanager

import android.content.Context
import java.io.File

/**
 * Provides static utility methods for constructing and creating
 * all the directories used by OfflineMap management.
 *
 * @since 200.8.0
 */
internal object OfflineURLs {

    /**
     * Retrieves the external directory path for storing application files (`<your-app-files-dir>`):
     *
     * - `/storage/emulated/0/Android/data/com.yourpackagename.appname/files/`
     *
     * @since 200.8.0
     */
    private fun getExternalDirPath(context: Context): String {
        return context.getExternalFilesDir(null)?.path.toString()
    }


    /**
     * Retrieves the external directory path for caching OfflineMapAreas files:
     *
     * - `/storage/emulated/0/Android/data/com.yourpackagename.appname/files/OfflineMapAreasCache`
     *
     * @since 200.8.0
     */
    private fun getOfflineCacheDirPath(context: Context): String {
        return File(getExternalDirPath(context), offlineMapAreasCacheDir).path.toString()
    }

    /**
     * Returns the path to the offline repository directory,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline`
     *
     * @since 200.8.0
     */
    internal fun offlineRepositoryDirectoryPath(context: Context): String {
        val dir = File(getExternalDirPath(context), offlineRepositoryDir)
            .makeDirectoryIfItDoesNotExist()
        return dir.absolutePath
    }

    /**
     * Returns the path to the web‐map directory for a specific portal item ID,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>`
     *
     * @since 200.8.0
     */
    internal fun portalItemDirectoryPath(context: Context, portalItemID: String): String {
        val base = File(offlineRepositoryDirectoryPath(context))
        val itemDir = File(base, portalItemID).makeDirectoryIfItDoesNotExist()
        return itemDir.absolutePath
    }

    /**
     * Returns the path to the “Preplanned” subdirectory for a portal item,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/Preplanned/<preplannedMapAreaID>`
     *
     * If [preplannedMapAreaID] is null:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/Preplanned`
     *
     * @since 200.8.0
     */
    internal fun prePlannedDirectoryPath(
        context: Context,
        portalItemID: String,
        preplannedMapAreaID: String? = null
    ): String {
        val itemDir = File(portalItemDirectoryPath(context, portalItemID))
        val preplannedDir = File(itemDir, preplannedMapAreas).makeDirectoryIfItDoesNotExist()
        return if (preplannedMapAreaID != null) {
            val areaDir = File(preplannedDir, preplannedMapAreaID).makeDirectoryIfItDoesNotExist()
            areaDir.absolutePath
        } else {
            preplannedDir.absolutePath
        }
    }

    /**
     * Returns the path to the “OnDemand” subdirectory for a portal item,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/OnDemand/<onDemandMapAreaID>`
     *
     *
     * If [onDemandMapAreaID] is null:
     *
     * - `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/OnDemand`
     *
     * @since 200.8.0
     */
    internal fun onDemandDirectoryPath(
        context: Context,
        portalItemID: String,
        onDemandMapAreaID: String? = null
    ): String {
        val itemDir = File(portalItemDirectoryPath(context, portalItemID))
        val onDemandDir = File(itemDir, onDemandAreas).makeDirectoryIfItDoesNotExist()
        return if (onDemandMapAreaID != null) {
            val areaDir = File(onDemandDir, onDemandMapAreaID).makeDirectoryIfItDoesNotExist()
            areaDir.absolutePath
        } else {
            onDemandDir.absolutePath
        }
    }

    /**
     * Returns the path to the “PendingMapInfo” directory from the external cache,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/OfflineMapAreasCache/PendingMapInfo/<portalItemID>/...`
     *
     * @since 200.8.0
     */
    internal fun pendingMapInfoDirectoryPath(context: Context, portalItemID: String): String {
        val caches = getOfflineCacheDirPath(context)
        val pendingBase = File(caches, pendingMapInfoDir).makeDirectoryIfItDoesNotExist()
        val itemPendingDir = File(pendingBase, portalItemID).makeDirectoryIfItDoesNotExist()
        return itemPendingDir.absolutePath
    }

    /**
     * Returns the path to the map area's metadata directory from the external cache,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/OfflineMapAreasCache/PendingMapInfo/<portalItemID>/<mapAreaID>/`
     *
     * @since 200.8.0
     */
    internal fun pendingAreaMetadataDirectoryPath(
        context: Context,
        portalItemID: String,
        mapAreaItemID: String
    ): String {
        val caches = getOfflineCacheDirPath(context)
        val pendingBase = File(caches, pendingMapInfoDir).makeDirectoryIfItDoesNotExist()
        val itemPendingDir = File(pendingBase, portalItemID).makeDirectoryIfItDoesNotExist()
        val areaPendingDir = File(itemPendingDir, mapAreaItemID).makeDirectoryIfItDoesNotExist()
        return areaPendingDir.absolutePath
    }

    /**
     * Returns the path to the “PendingMapInfo” directory from the external cache,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/OfflineMapAreasCache/PendingJobs/<portalItemID>/`
     *
     * @since 200.8.0
     */
    internal fun pendingJobInfoDirectoryPath(context: Context, portalItemID: String): String {
        val caches = getOfflineCacheDirPath(context)
        val pendingBase = File(caches, pendingJobsDir).makeDirectoryIfItDoesNotExist()
        val itemPendingDir = File(pendingBase, portalItemID).makeDirectoryIfItDoesNotExist()
        return itemPendingDir.absolutePath
    }

    private fun File.makeDirectoryIfItDoesNotExist(): File {
        if (!exists()) mkdirs()
        return this
    }
}
