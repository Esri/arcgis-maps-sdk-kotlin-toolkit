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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedPackagingStatus
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.jobAreaTitleKey
import com.arcgismaps.toolkit.offline.jobParameter
import com.arcgismaps.toolkit.offline.notificationIdParameter
import com.arcgismaps.toolkit.offline.offlineJobJsonFile
import com.arcgismaps.toolkit.offline.offlineMapFile
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.uniqueWorkName
import com.arcgismaps.toolkit.offline.workmanager.OfflineJobWorker
import com.arcgismaps.toolkit.offline.workmanager.logWorkInfos
import com.arcgismaps.toolkit.offline.workmanager.observeStatusForPreplannedWork
import java.io.File
import kotlin.random.Random

/**
 * Represents the state of a [PreplannedMapArea].
 *
 * @since 200.8.0
 */
internal class PreplannedMapAreaState(
    internal val preplannedMapArea: PreplannedMapArea,
    private val offlineMapTask: OfflineMapTask,
    private val getExternalFilesDirPath: String,
    private val workManager : WorkManager
) {
    // The status of the preplanned map area.
    var status: Status = Status.NotLoaded

    private var preplannedMapAreaTitle = ""

    // TODO: This path is reset prior to a job, next, define local repository rules for offline map files.
    private val offlineMapPath by lazy {
        getExternalFilesDirPath + File.separator + offlineMapFile
    }

    // create a temporary file path to save the offlineMapJob json file
    private val offlineJobJsonPath by lazy {
        getExternalFilesDirPath + File.separator + offlineJobJsonFile
    }

    internal suspend fun initialize() = runCatchingCancellable {
        preplannedMapArea.load()
            .onSuccess {
                status = try {
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
            status = Status.Downloading
            startOfflineMapJob(
                downloadPreplannedOfflineMapJob = createOfflineMapJob(
                    preplannedMapArea = preplannedMapArea
                )
            )
            // Start observing WorkManager status
            observeStatusForPreplannedWork(
                workManager = workManager,
                onWorkInfoStateChanged = ::logWorkInfos,
                preplannedMapAreaState = this
            )

        } catch (e: Exception) {
            Log.e("OfflineMapState", "Error taking preplanned map offline", e)
            status = Status.DownloadFailure(e)
        }
    }

    private suspend fun createOfflineMapJob(
        preplannedMapArea: PreplannedMapArea
    ): DownloadPreplannedOfflineMapJob {
        // Check and delete if the offline map package file already exists
        File(offlineMapPath).deleteRecursively()

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
            preplannedMapArea = preplannedMapArea
        ).getOrThrow().apply {
            // Set the update mode to receive no updates
            updateMode = PreplannedUpdateMode.NoUpdates
            continueOnErrors = false
        }

        // Define the path where the map will be saved
        val downloadDirectoryPath = offlineMapPath + File.separator + preplannedMapArea.portalItem
        File(downloadDirectoryPath).mkdirs()
        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
            parameters = params,
            downloadDirectoryPath = downloadDirectoryPath
        )

        return downloadPreplannedOfflineMapJob
    }

    /**
     * Starts the [downloadPreplannedOfflineMapJob] using OfflineJobWorker with WorkManager.
     * The [downloadPreplannedOfflineMapJob] is serialized into a json file and the uri is passed
     * to the OfflineJobWorker, since WorkManager enforces a MAX_DATA_BYTES for the WorkRequest's data
     */
    private fun startOfflineMapJob(downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob) {
        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(downloadPreplannedOfflineMapJob.toJson())

        // create a non-zero notification id for the OfflineJobWorker
        // this id will be used to post or update any progress/status notifications
        val notificationId = Random.nextInt(1, 100)


        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<OfflineJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the input data
            .setInputData(
                // add the notificationId and the json file path as a key/value pair
                workDataOf(
                    notificationIdParameter to notificationId,
                    jobParameter to offlineJobJsonFile.absolutePath,
                    jobAreaTitleKey to preplannedMapAreaTitle
                )
            ).build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
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
