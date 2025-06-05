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

    internal fun getExternalDirPath(context: Context): String {
        return context.getExternalFilesDir(null)?.path.toString()
    }

    /**
     * Returns the path to the offline manager directory,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager`
     *
     * @since 200.8.0
     */
    internal fun offlineManagerDirectory(context: Context): String {
        val dir = File(context.filesDir, offlineManagerDir).mkdirsIfNotExists()
        return dir.absolutePath
    }

    /**
     * Returns the path to the web‐map directory for a specific portal item ID,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>`
     *
     * @since 200.8.0
     */
    internal fun portalItemDirectory(context: Context, portalItemID: String): String {
        val base = File(offlineManagerDirectory(context))
        val itemDir = File(base, portalItemID).mkdirsIfNotExists()
        return itemDir.absolutePath
    }

    /**
     * Returns the path to the “Preplanned” subdirectory for a portal item,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>/Preplanned/<preplannedMapAreaID>`
     *
     * If [preplannedMapAreaID] is null:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>/Preplanned`
     *
     * @since 200.8.0
     */
    internal fun prePlannedDirectory(
        context: Context,
        portalItemID: String,
        preplannedMapAreaID: String? = null
    ): String {
        val itemDir = File(portalItemDirectory(context, portalItemID))
        val preplannedDir = File(itemDir, preplannedMapAreas).mkdirsIfNotExists()
        return if (preplannedMapAreaID != null) {
            val areaDir = File(preplannedDir, preplannedMapAreaID).mkdirsIfNotExists()
            areaDir.absolutePath
        } else {
            preplannedDir.absolutePath
        }
    }

    /**
     * Returns the path to the “OnDemand” subdirectory for a portal item,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>/OnDemand/<onDemandMapAreaID>`
     *
     *
     * If [onDemandMapAreaID] is null:
     *
     * - `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>/OnDemand`
     *
     * @since 200.8.0
     */
    internal fun onDemandDirectory(
        context: Context,
        portalItemID: String,
        onDemandMapAreaID: String? = null
    ): String {
        val itemDir = File(portalItemDirectory(context, portalItemID))
        val onDemandDir = File(itemDir, onDemandAreas).mkdirsIfNotExists()
        return if (onDemandMapAreaID != null) {
            val areaDir = File(onDemandDir, onDemandMapAreaID).mkdirsIfNotExists()
            areaDir.absolutePath
        } else {
            onDemandDir.absolutePath
        }
    }

    /**
     * Returns the path to the “PendingMapInfo” directory in app caches,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-cache>/PendingMapInfo/<portalItemID>/...`
     *
     * @since 200.8.0
     */
    internal fun pendingMapInfoDirectory(context: Context, portalItemID: String): File {
        val caches = context.cacheDir
        val pendingBase = File(caches, pendingMapInfoDir).mkdirsIfNotExists()
        val itemPendingDir = File(pendingBase, portalItemID).mkdirsIfNotExists()
        return itemPendingDir
    }

    /**
     * Returns the path to the “PendingMapInfo” directory in app caches,
     * creates the directory if it doesn’t already exist:
     *
     * - `<your-app-cache>/PendingJobs/<portalItemID>/`
     *
     * @since 200.8.0
     */
    internal fun pendingJobInfoDirectory(context: Context, portalItemID: String): String {
        val caches = context.cacheDir
        val pendingBase = File(caches, pendingJobsDir).mkdirsIfNotExists()
        val itemPendingDir = File(pendingBase, portalItemID).mkdirsIfNotExists()
        return itemPendingDir.absolutePath
    }

    private fun File.mkdirsIfNotExists(): File {
        if (!exists()) mkdirs()
        return this
    }
}
