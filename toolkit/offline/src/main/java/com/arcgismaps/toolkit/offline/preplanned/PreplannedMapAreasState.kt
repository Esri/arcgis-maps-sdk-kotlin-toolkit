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

package com.arcgismaps.toolkit.offline.preplanned

import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedPackagingStatus
import com.arcgismaps.toolkit.offline.runCatchingCancellable

/**
 * Represents the state of the preplanned map areas.
 *
 * @since 200.8.0
 */
internal class PreplannedMapAreaState(internal val preplannedMapArea: PreplannedMapArea) {
    // The status of the preplanned map area.
    var status: Status = Status.NotLoaded

    // The offline map task.
    var offlineMapTask: OfflineMapTask? = null

    internal suspend fun initialize() = runCatchingCancellable {
        preplannedMapArea.load()
            .onSuccess {
                status = try {
                    // Note: Packaging status is `Unknown` for compatibility with legacy webmaps
                    // that have incomplete metadata.
                    // If the area loads, then you know for certain the status is complete.
                    Status.fromPackagingStatus(preplannedMapArea.packagingStatus)
                } catch (illegalStateException: IllegalStateException) {
                    Status.Packaged
                }
            preplannedMapArea.portalItem.thumbnail?.load()
        }
    }
}

internal sealed class Status {

    // Preplanned map area not loaded.
    data object NotLoaded : Status()

    // Preplanned map area is loading.
    data object Loading : Status()

    // Preplanned map area failed to load.
    data class LoadFailure(val error: Throwable) : Status()

    // Preplanned map area is packaging.
    data object Packaging : Status()

    // Preplanned map area is packaged and ready for download.
    data object Packaged : Status()

    // Preplanned map area packaging failed.
    data object PackageFailure : Status()

    // Preplanned map area is being downloaded.
    data object Downloading : Status()

    // Preplanned map area is downloaded.
    data object Downloaded : Status()

    // Preplanned map area failed to download.
    data class DownloadFailure(val error: Throwable) : Status()

    // Downloaded mobile map package failed to load.
    data class MmpkLoadFailure(val error: Throwable) : Status()

    companion object {
        fun fromPackagingStatus(packagingStatus: PreplannedPackagingStatus): Status {
            return when (packagingStatus) {
                PreplannedPackagingStatus.Processing -> Packaging
                PreplannedPackagingStatus.Failed -> PackageFailure
                PreplannedPackagingStatus.Complete -> Packaged
                PreplannedPackagingStatus.Unknown -> throw IllegalStateException("Unknown packaging status")
            }
        }
    }

    // Indicates whether the model can load the preplanned map area.
    val canLoadPreplannedMapArea: Boolean
        get() = when (this) {
            is NotLoaded, is LoadFailure, is PackageFailure -> true
            is Loading, is Packaging, is Packaged, is Downloading, is Downloaded, is MmpkLoadFailure, is DownloadFailure -> false
        }

    // Indicates if download is allowed for this status.
    val allowsDownload: Boolean
        get() = when (this) {
            is Packaged, is DownloadFailure -> true
            is NotLoaded, is Loading, is LoadFailure, is Packaging, is PackageFailure, is Downloading, is Downloaded, is MmpkLoadFailure -> false
        }

    // Indicates whether the preplanned map area is downloaded.
    val isDownloaded: Boolean
        get() = this is Downloaded
}
