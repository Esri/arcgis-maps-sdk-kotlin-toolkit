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

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedPackagingStatus
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.WorkManagerRepository
import com.arcgismaps.toolkit.offline.preplannedMapAreas
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.workmanager.logWorkInfos
import java.io.File
import java.util.UUID

/**
 * Represents the state of a [PreplannedMapArea].
 *
 * @since 200.8.0
 */
internal class PreplannedMapAreaState(
    internal val preplannedMapArea: PreplannedMapArea,
    private val offlineMapTask: OfflineMapTask,
    private val workManagerRepository: WorkManagerRepository
) {
    // The status of the preplanned map area.
    private var _status by mutableStateOf<Status>(Status.NotLoaded)
    internal val status: Status
        get() = _status

    private var preplannedMapAreaTitle = ""

    internal suspend fun initialize() = runCatchingCancellable {
        preplannedMapArea.load()
            .onSuccess {
                _status = try {
                    Status.fromPackagingStatus(preplannedMapArea.packagingStatus)
                } catch (illegalStateException: IllegalStateException) {
                    // Note: Packaging status is `Unknown` for compatibility with legacy webmaps
                    // that have incomplete metadata. We throw an illegalStateException when Package
                    // Status is unknown. We can safely assume that the preplanned map area is packaged.
                    // If the area loads, then we know for certain the status is complete.
                    Status.Packaged
                }
                // Load the thumbnail
                preplannedMapArea.portalItem.thumbnail?.load()
                // Set preplanned area title
                preplannedMapAreaTitle = preplannedMapArea.portalItem.title
            }
    }

    internal suspend fun downloadPreplannedMapArea() {
        try {
            // Set the downloading status
            _status = Status.Downloading
            val offlineWorkerUUID = startOfflineMapJob(
                downloadPreplannedOfflineMapJob = createOfflineMapJob(
                    preplannedMapArea = preplannedMapArea
                )
            )
            // Start observing WorkManager status
            workManagerRepository.observeStatusForPreplannedWork(
                onWorkInfoStateChanged = ::logWorkInfos,
                preplannedMapAreaState = this,
                offlineWorkerUUID = offlineWorkerUUID
            )

        } catch (e: Exception) {
            Log.e("Offline: PreplannedMapAreaState", "Error taking preplanned map offline", e)
            _status = Status.DownloadFailure(e)
        }
    }

    private suspend fun createOfflineMapJob(
        preplannedMapArea: PreplannedMapArea
    ): DownloadPreplannedOfflineMapJob {
        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
            preplannedMapArea = preplannedMapArea
        ).getOrThrow().apply {
            // Set the update mode to receive no updates
            updateMode = PreplannedUpdateMode.NoUpdates
            continueOnErrors = false
        }

        // Define the path where the map will be saved
        val preplannedMapAreaDownloadDirectory = workManagerRepository.createContentsForPath(
            offlineMapDirectoryName = preplannedMapAreas + File.separator + preplannedMapArea.portalItem.itemId
        )

        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
            parameters = params,
            downloadDirectoryPath = preplannedMapAreaDownloadDirectory.path
        )

        return downloadPreplannedOfflineMapJob
    }

    /**
     * Starts the [downloadPreplannedOfflineMapJob] using OfflineJobWorker with WorkManager.
     * The [downloadPreplannedOfflineMapJob] is serialized into a json file and the uri is passed
     * to the OfflineJobWorker, since WorkManager enforces a MAX_DATA_BYTES for the WorkRequest's data
     */
    private fun startOfflineMapJob(downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob): UUID {
        val jsonJobFile = workManagerRepository.saveJobToDisk(
            jobPath = preplannedMapAreas + File.separator + "${preplannedMapArea.portalItem.title}.json",
            jobJson =  downloadPreplannedOfflineMapJob.toJson()
        )

        val workerUUID  = workManagerRepository.createPreplannedMapAreaRequestAndQueDownload(
            notificationId = workManagerRepository.createNotificationIdForJob(),
            jsonJobPath = jsonJobFile.path,
            preplannedMapAreaTitle = preplannedMapArea.portalItem.title
        )

        workManagerRepository.getProgressForUUID(workerUUID)

        return workerUUID
    }

    internal fun updateStatus(newStatus: Status) {
        _status = newStatus
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
