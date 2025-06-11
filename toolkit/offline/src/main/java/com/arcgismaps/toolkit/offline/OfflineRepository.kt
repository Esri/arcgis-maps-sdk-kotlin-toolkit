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

package com.arcgismaps.toolkit.offline

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.Status
import com.arcgismaps.toolkit.offline.workmanager.OfflineURLs
import com.arcgismaps.toolkit.offline.workmanager.PreplannedMapAreaJobWorker
import com.arcgismaps.toolkit.offline.workmanager.downloadJobJsonFile
import com.arcgismaps.toolkit.offline.workmanager.jobAreaTitleKey
import com.arcgismaps.toolkit.offline.workmanager.jobWorkerUuidKey
import com.arcgismaps.toolkit.offline.workmanager.jsonJobPathKey
import com.arcgismaps.toolkit.offline.workmanager.mobileMapPackagePathKey
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoJsonFile
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoThumbnailFile
import com.arcgismaps.toolkit.offline.workmanager.preplannedMapAreas
import java.io.File
import java.util.UUID

/**
 * Manages WorkManager operations for offline map tasks, including job creation, queuing,
 * progress tracking, and cleanup. Provides utilities to handle serialization of jobs,
 * manage notifications, and observe job states for preplanned and onDemand map areas.
 *
 * @since 200.8.0
 */
public class OfflineRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    private var _offlineMapInfos: MutableList<OfflineMapInfo> = mutableListOf()

    /**
     * The portal item information for web maps that have downloaded map areas.
     *
     * @since 200.8.0
     */
    public val offlineMapInfos: List<OfflineMapInfo>
        get() = _offlineMapInfos.toList()

    init {
        _offlineMapInfos.addAll(loadOfflineMapInfos())
    }

    /**
     * Removes all the offlineMapInfos and downloads from the disk.
     *
     * @since 200.8.0
     */
    public fun removeAllDownloads() {
        _offlineMapInfos.clear()
        val baseDir = File(OfflineURLs.offlineManagerDirectory(context))
        if (baseDir.exists()) {
            baseDir.deleteRecursively()
        }
    }

    /**
     * Removes all downloads for a specific web map's [OfflineMapInfo].
     * Deletes the corresponding directory and removes the info from the list.
     *
     * @param offlineMapInfo The [OfflineMapInfo] to remove.
     * @since 200.8.0
     */
    public fun removeDownloadsForWebmap(offlineMapInfo: OfflineMapInfo) {
        _offlineMapInfos.remove(offlineMapInfo)
        val baseDir = File(OfflineURLs.offlineManagerDirectory(context))
        val offlineMapInfoDir = File(baseDir, offlineMapInfo.id)
        if (offlineMapInfoDir.exists()) {
            offlineMapInfoDir.deleteRecursively()
        }
    }

    /**
     * Saves the [OfflineMapInfo] to the pending folder for a particular web map's portal item.
     * The info will stay in that folder until the job completes.
     */
    private fun savePendingMapInfo(portalItem: PortalItem) {
        val pendingMapInfoDir = OfflineURLs.pendingMapInfoDirectory(context, portalItem.itemId)
        if (!OfflineMapInfo.isSerializedFilePresent(pendingMapInfoDir)) {
            val info = OfflineMapInfo(portalItem)
            info.saveToDirectory(pendingMapInfoDir)
        }
    }

    /**
     * Creates and returns the directory file for a pending preplanned job.
     *
     * @param portalItemID The ID of the portal item for which to create a pending job folder.
     * @param preplannedMapAreaID The ID of the specific preplanned map area under the portal item.
     * @return A [File] instance pointing to the created `<portalItemID>/<preplannedMapAreaID>` directory.
     * @since 200.8.0
     */
    internal fun createPendingPreplannedJobPath(
        portalItemID: String,
        preplannedMapAreaID: String
    ): File {
        return File(
            OfflineURLs.pendingJobInfoDirectory(context, portalItemID),
            preplannedMapAreaID
        ).also { it.mkdirs() }
    }

    /**
     * Saves a serialized offline map job as a JSON file on disk. Ensures directories are created
     * and writes the provided JSON content to the specified path.
     *
     * @param jobPath The relative path where the JSON file should be saved.
     * @param jobJson The serialized string representation of the offline map job.
     * @return A [File] instance pointing to the saved JSON file.
     *
     * @since 200.8.0
     */
    internal fun saveJobToDisk(jobPath: String, jobJson: String): File {
        // create the job pending dir
        val offlineJobJsonFile = File(jobPath, downloadJobJsonFile)
        offlineJobJsonFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(jobJson)
        return offlineJobJsonFile
    }

    /**
     * Returns the list of [OfflineMapInfo] available from disk.
     *
     * @since 200.8.0
     */
    private fun loadOfflineMapInfos(): List<OfflineMapInfo> {
        val baseDir = File(OfflineURLs.offlineManagerDirectory(context))
        val offlineMapInfos = mutableListOf<OfflineMapInfo>()
        if (!baseDir.exists() || !baseDir.isDirectory) {
            return offlineMapInfos
        }
        val entries: Array<File> = baseDir.listFiles() ?: return offlineMapInfos
        for (fileEntry in entries) {
            if (!fileEntry.isDirectory || fileEntry.name.equals(offlineMapInfoJsonFile)) {
                continue
            }
            val info = OfflineMapInfo.createFromDirectory(fileEntry) ?: continue
            offlineMapInfos.add(info)
        }
        return offlineMapInfos
    }

    /**
     * Returns the path to the final “Preplanned/<areaItemID>” folder.
     * Moves all contents from: `<your-app-cache>/PendingJobs/<portalItemID>/<areaItemID>`
     * into: `<your-app-files-dir>/com.esri.ArcGISToolkit.offlineManager/<portalItemID>/Preplanned/<areaItemID>`
     *
     * @since 200.8.0
     */
    private fun movePreplannedJobResultToDestination(offlineMapCacheDownloadPath: String): File {
        val cacheAreaDir = File(offlineMapCacheDownloadPath)
        val areaItemID = cacheAreaDir.name
        val portalDir = cacheAreaDir.parentFile
        val portalItemID = portalDir?.name.toString()
        val destDirPath = OfflineURLs.prePlannedDirectory(
            context = context,
            portalItemID = portalItemID,
            preplannedMapAreaID = areaItemID
        )
        val destDir = File(destDirPath)
        cacheAreaDir.listFiles()?.forEach { child ->
            val target = File(destDir, child.name)
            child.copyRecursively(target, overwrite = true)
        }
        movePreplannedOfflineMapInfoToDestination(portalItemID)
        cacheAreaDir.deleteRecursively()
        return destDir
    }

    internal fun deleteContentsForDirectory(offlineMapDirectoryName: String): Boolean {
        return File(offlineMapDirectoryName).deleteRecursively()
    }

    /**
     * Removes the offline map information for a given portal item ID.
     * Deletes the info.json and thumbnail file from the corresponding directory
     * and removes the info from the list.
     *
     * @param portalItemID The ID of the portal item whose offline map info should be removed.
     * @since 200.8.0
     */
    internal fun removeOfflineMapInfo(portalItemID: String) {
        _offlineMapInfos.removeAll { it.id == portalItemID }
        val baseDir = File(OfflineURLs.offlineManagerDirectory(context))
        val offlineMapInfoDir = File(baseDir, portalItemID)
        OfflineMapInfo.removeFromDirectory(offlineMapInfoDir)
    }

    private fun movePreplannedOfflineMapInfoToDestination(portalItemID: String) {
        val pendingDir = OfflineURLs.pendingMapInfoDirectory(context, portalItemID)
        val infoFile = File(pendingDir, offlineMapInfoJsonFile)
        val destDirPath = OfflineURLs.portalItemDirectory(
            context = context,
            portalItemID = portalItemID
        )
        if (infoFile.exists()) {
            infoFile.copyRecursively(File(destDirPath, offlineMapInfoJsonFile), overwrite = true)
        }
        val thumbnailFile = File(pendingDir, offlineMapInfoThumbnailFile)
        if (thumbnailFile.exists()) {
            thumbnailFile.copyRecursively(
                File(destDirPath, offlineMapInfoThumbnailFile),
                overwrite = true
            )
        }
        pendingDir.deleteRecursively()
    }

    /**
     * Checks whether a given [preplannedMapAreaID] associated with a [portalItemID]
     * has already been downloaded locally.
     *
     * @return The path to the preplanned area’s local folder if it exists,
     *         otherwise `null`.
     * @since 200.8.0
     */
    internal fun isPrePlannedAreaDownloaded(
        portalItemID: String,
        preplannedMapAreaID: String
    ): String? {
        val destDir = File(
            File(OfflineURLs.portalItemDirectory(context, portalItemID), preplannedMapAreas),
            preplannedMapAreaID
        )
        return if (destDir.exists())
            destDir.path
        else null
    }

    /**
     * Creates and enqueues a one-time WorkManager request for downloading an offline map area
     * using [PreplannedMapAreaJobWorker]. Sets up expedited work with input data containing
     * notification and job details. Ensures only one worker instance runs at any time by
     * replacing active workers with the same unique name as per the defined policy.
     *
     * @param jsonJobPath The file path to the serialized JSON representation of the job.
     * @param preplannedMapAreaTitle The title of the preplanned map area being downloaded.
     * @return A [UUID] representing the identifier of the enqueued WorkManager request.
     * @since 200.8.0
     */
    internal fun createPreplannedMapAreaRequestAndQueueDownload(
        jsonJobPath: String,
        preplannedMapAreaTitle: String
    ): UUID {
        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<PreplannedMapAreaJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the input data
            .setInputData(
                // add the notificationId and the json file path as a key/value pair
                workDataOf(
                    jsonJobPathKey to jsonJobPath,
                    jobAreaTitleKey to preplannedMapAreaTitle
                )
            ).build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        workManager.enqueueUniqueWork(
            uniqueWorkName = jobWorkerUuidKey + workRequest.id,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = workRequest
        )
        return workRequest.id
    }

    /**
     * Observes updates on a specific WorkManager task identified by its UUID and
     * handles status changes for preplanned map areas. Monitors task states to update
     * corresponding statuses in [PreplannedMapAreaState]. Finally, prunes completed tasks
     * from WorkManager's database when necessary.
     *
     *
     * @param offlineWorkerUUID The unique identifier associated with the specific task being observed in WorkManager.
     * @param preplannedMapAreaState The [PreplannedMapAreaState] instance to update based on task progress or completion.
     * @param onWorkInfoStateChanged A callback function triggered when work state changes occur.
     * @since 200.8.0
     */
    internal suspend fun observeStatusForPreplannedWork(
        offlineWorkerUUID: UUID,
        preplannedMapAreaState: PreplannedMapAreaState,
        portalItem: PortalItem,
        onWorkInfoStateChanged: (WorkInfo) -> Unit,
    ) {
        savePendingMapInfo(portalItem)
        // collect the flow to get the latest work info list
        workManager.getWorkInfoByIdFlow(offlineWorkerUUID)
            .collect { workInfo ->
                if (workInfo != null) {
                    // Report progress
                    val progress = workInfo.progress.getInt("Progress", 0)
                    preplannedMapAreaState.updateDownloadProgress(progress)

                    // emit changes in the work info state
                    onWorkInfoStateChanged(workInfo)
                    // check the current state of the work request
                    when (workInfo.state) {
                        // if work completed successfully
                        WorkInfo.State.SUCCEEDED -> {
                            preplannedMapAreaState.updateStatus(Status.Downloaded)
                            workInfo.outputData.getString(mobileMapPackagePathKey)?.let { path ->
                                // using the pending path, move the result to final destination path
                                val destDir = movePreplannedJobResultToDestination(path)
                                // create & load the downloaded map
                                preplannedMapAreaState.createAndLoadMMPKAndOfflineMap(
                                    mobileMapPackagePath = destDir.absolutePath
                                )
                                // create offline map information from local directory
                                OfflineMapInfo.createFromDirectory(
                                    directory = File(
                                        OfflineURLs.portalItemDirectory(
                                            context = context,
                                            portalItemID = portalItem.itemId
                                        )
                                    )
                                )?.let {
                                    // if non-null info was created, add it to the list
                                    _offlineMapInfos.add(it)
                                }
                            } ?: run {
                                preplannedMapAreaState.updateStatus(
                                    Status.MmpkLoadFailure(
                                        Exception("Mobile Map Package path is null")
                                    )
                                )
                            }
                            preplannedMapAreaState.disposeScope()
                        }
                        // if the work failed or was cancelled
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            // this removes the completed WorkInfo from the WorkManager's database
                            // otherwise, the observer will emit the WorkInfo on every launch
                            // until WorkManager auto-prunes
                            workManager.pruneWork()
                            preplannedMapAreaState.updateStatus(
                                Status.DownloadFailure(
                                    Exception(
                                        "${workInfo.tags}: FAILED. Reason: " +
                                                "${workInfo.outputData.getString("Error")}"
                                    )
                                )
                            )
                            preplannedMapAreaState.disposeScope()
                        }
                        // if the work is currently in progress
                        WorkInfo.State.RUNNING -> {
                            preplannedMapAreaState.updateStatus(Status.Downloading)
                        }
                        // don't have to handle other states
                        else -> {}
                    }
                }
            }
    }

    /**
     * Cancels a WorkManager request by its unique identifier (UUID).
     *
     * @param workerUUID The UUID of the WorkManager request to cancel.
     * @since 200.8.0
     */
    internal fun cancelWorkRequest(workerUUID: UUID) {
        workManager.cancelWorkById(workerUUID)
    }
}
