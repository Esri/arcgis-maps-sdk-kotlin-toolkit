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
import androidx.work.WorkQuery
import androidx.work.workDataOf
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreasState
import com.arcgismaps.toolkit.offline.ondemand.OnDemandStatus
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.PreplannedStatus
import com.arcgismaps.toolkit.offline.workmanager.OfflineURLs
import com.arcgismaps.toolkit.offline.workmanager.OnDemandMapAreaJobWorker
import com.arcgismaps.toolkit.offline.workmanager.PreplannedMapAreaJobWorker
import com.arcgismaps.toolkit.offline.workmanager.downloadJobJsonFile
import com.arcgismaps.toolkit.offline.workmanager.jobAreaTitleKey
import com.arcgismaps.toolkit.offline.workmanager.jobWorkerUuidKey
import com.arcgismaps.toolkit.offline.workmanager.jsonJobPathKey
import com.arcgismaps.toolkit.offline.workmanager.mobileMapPackagePathKey
import com.arcgismaps.toolkit.offline.workmanager.offlineAreaMetadataJsonFile
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoJsonFile
import com.arcgismaps.toolkit.offline.workmanager.offlineMapInfoThumbnailFile
import com.arcgismaps.toolkit.offline.workmanager.onDemandAreas
import com.arcgismaps.toolkit.offline.workmanager.preplannedMapAreas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Manages WorkManager operations for offline map tasks, including job creation, queuing,
 * progress tracking, and cleanup. Provides utilities to handle serialization of jobs,
 * manage notifications, and observe job states for preplanned and onDemand map areas.
 *
 * @since 200.8.0
 */
public object OfflineRepository {

    private var _offlineMapInfos: SnapshotStateList<OfflineMapInfo> = mutableStateListOf()

    /**
     * The portal item information for web maps that have downloaded map areas.
     *
     * @since 200.8.0
     */
    public val offlineMapInfos: List<OfflineMapInfo> = _offlineMapInfos

    /**
     * Initializes the offline map repository by loading existing offline map infos from disk.
     *
     * @param context The application context.
     * @since 200.8.0
     */
    public fun refreshOfflineMapInfos(context: Context) {
        _offlineMapInfos.clear()
        _offlineMapInfos.addAll(loadOfflineMapInfos(context))
    }

    /**
     * Removes all downloads for all offline maps from the disk and clears offline map infos.
     *
     * @since 200.8.0
     */
    public fun removeAllDownloads(context: Context) {
        _offlineMapInfos.clear()
        val baseDir = File(OfflineURLs.offlineRepositoryDirectoryPath(context))
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
    public fun removeDownloadsForWebmap(context: Context, offlineMapInfo: OfflineMapInfo) {
        _offlineMapInfos.remove(offlineMapInfo)
        val baseDir = File(OfflineURLs.offlineRepositoryDirectoryPath(context))
        val offlineMapInfoDir = File(baseDir, offlineMapInfo.id)
        if (offlineMapInfoDir.exists()) {
            offlineMapInfoDir.deleteRecursively()
        }
    }

    /**
     * Saves the [OfflineMapInfo] to the pending folder for a particular web map's portal item.
     * The info will stay in that folder until the job completes.
     *
     * - `<your-app-files-dir>/OfflineMapAreasCache/PendingMapInfo/<portalItemID>/info.json`
     *
     * @since 200.8.0
     */
    private fun savePendingMapInfo(context: Context, portalItem: PortalItem) {
        val pendingMapInfoDir = File(
            OfflineURLs.pendingMapInfoDirectoryPath(context, portalItem.itemId)
        )
        if (!OfflineMapInfo.isSerializedFilePresent(pendingMapInfoDir)) {
            val info = OfflineMapInfo(portalItem)
            info.saveToDirectory(pendingMapInfoDir)
        }
    }

    /**
     * Saves the [OfflineMapAreaMetadata] to the pending folder for map area of a web map's portal item.
     * The info will stay in this folder until the job completes.
     *
     * - `<your-app-files-dir>/OfflineMapAreasCache/PendingMapInfo/<portalItemID>/<areaItemID>/metadata.json`
     *
     * @since 200.8.0
     */
    private fun savePendingMapAreaMetadata(
        context: Context,
        portalItem: PortalItem,
        areaMetadata: OfflineMapAreaMetadata
    ) {
        val pendingAreaMetadataDir = File(
            OfflineURLs.pendingAreaMetadataDirectoryPath(
                context, portalItem.itemId, areaMetadata.areaId
            )
        )
        if (!OfflineMapAreaMetadata.isSerializedFilePresent(pendingAreaMetadataDir)) {
            areaMetadata.saveToDirectory(pendingAreaMetadataDir)
        }
    }
    /**
     * Returns preplanned/on-demand map area [OfflineMapAreaMetadata] using the corresponding job [UUID].
     *
     * @since 200.8.0
     */
    internal suspend fun getMapAreaMetadataForOfflineJob(
        context: Context,
        uuid: UUID,
        portalItemId: String
    ): OfflineMapAreaMetadata? {
        val workManager = WorkManager.getInstance(context)
        val workQuery = WorkQuery.Builder
            .fromIds(listOf(uuid))
            .build()
        val workInfos = withContext(Dispatchers.IO) {
            workManager.getWorkInfos(workQuery).get()
        }
        val workerTags = workInfos.firstOrNull()?.tags ?: return null
        workerTags.forEach { tag ->
            // Skip non relevant tags, like: com.arcgismaps.toolkit.offline.workmanager.
            if (tag != portalItemId && tag.length < 42) {
                val areaMetadataDir = File(
                    OfflineURLs.pendingAreaMetadataDirectoryPath(
                        context, portalItemId, tag
                    )
                )
                if (OfflineMapAreaMetadata.isSerializedFilePresent(areaMetadataDir)) {
                    return OfflineMapAreaMetadata.createFromDirectory(areaMetadataDir)
                }
            }
        }
        return null
    }

    /**
     * Returns the list of [UUID] for active running/enqueued jobs for the given [portalItemId].
     *
     * @since 200.8.0
     */
    internal suspend fun getActiveOfflineJobs(
        context: Context,
        portalItemId: String
    ): List<UUID> {
        val workManager = WorkManager.getInstance(context)
        val workQuery = WorkQuery.Builder
            .fromTags(listOf(portalItemId))
            .build()
        val workInfos = withContext(Dispatchers.IO) {
            workManager.getWorkInfos(workQuery).get()
        }
        val activePortalItemWorkers = workInfos.filter { workInfo ->
            (workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING)
        }
        return activePortalItemWorkers.map { it.id }
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
        context: Context,
        portalItemID: String,
        preplannedMapAreaID: String
    ): File {
        return File(
            OfflineURLs.pendingJobInfoDirectoryPath(context, portalItemID),
            preplannedMapAreaID
        ).also { it.mkdirs() }
    }

    /**
     * Creates and returns the directory file for a pending on-demand job.
     *
     * @param portalItemID The ID of the portal item for which to create a pending job folder.
     * @param onDemandMapAreaID The ID of the specific on-demand map area.
     * @return A [File] instance pointing to the created `<portalItemID>/<onDemandMapAreaID>` directory.
     * @since 200.8.0
     */
    internal fun createPendingOnDemandJobPath(
        context: Context,
        portalItemID: String,
        onDemandMapAreaID: String
    ): File {
        return File(
            OfflineURLs.pendingJobInfoDirectoryPath(context, portalItemID),
            onDemandMapAreaID
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
    internal fun loadOfflineMapInfos(context: Context): List<OfflineMapInfo> {
        val baseDir = File(OfflineURLs.offlineRepositoryDirectoryPath(context))
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
     *
     * - Moves all contents from: `<your-app-files-dir>/PendingJobs/<portalItemID>/<areaItemID>`
     * - into: `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/Preplanned/<areaItemID>`
     *
     * @since 200.8.0
     */
    internal fun movePreplannedJobResultToDestination(
        context: Context,
        offlineMapCacheDownloadPath: String
    ): File {
        val cacheAreaDir = File(offlineMapCacheDownloadPath)
        val areaItemID = cacheAreaDir.name
        val portalDir = cacheAreaDir.parentFile
        val portalItemID = portalDir?.name.toString()
        val destDirPath = OfflineURLs.prePlannedDirectoryPath(
            context = context,
            portalItemID = portalItemID,
            preplannedMapAreaID = areaItemID,
            isMakeDirsEnabled = true
        ).toString()
        val destDir = File(destDirPath)
        cacheAreaDir.listFiles()?.forEach { child ->
            val target = File(destDir, child.name)
            child.copyRecursively(target, overwrite = true)
        }
        moveOfflineMapInfoToDestination(context, portalItemID)
        cacheAreaDir.deleteRecursively()
        return destDir
    }

    /**
     * Returns the path to the final “OnDemand/<areaItemID>” folder.
     *
     * - Moves all contents from: `<your-app-files-dir>/PendingJobs/<portalItemID>/<areaItemID>`
     * - into: `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/OnDemand/<areaItemID>`
     *
     * @since 200.8.0
     */
    internal fun moveOnDemandJobResultToDestination(
        context: Context,
        offlineMapCacheDownloadPath: String
    ): File {
        val cacheAreaDir = File(offlineMapCacheDownloadPath)
        val areaItemID = cacheAreaDir.name
        val portalDir = cacheAreaDir.parentFile
        val portalItemID = portalDir?.name.toString()
        val destDirPath = OfflineURLs.onDemandDirectoryPath(
            context = context,
            portalItemID = portalItemID,
            onDemandMapAreaID = areaItemID
        )
        val destDir = File(destDirPath)
        cacheAreaDir.listFiles()?.forEach { child ->
            val target = File(destDir, child.name)
            child.copyRecursively(target, overwrite = true)
        }
        moveOfflineMapInfoToDestination(context, portalItemID)
        cacheAreaDir.deleteRecursively()
        return destDir
    }

    /**
     * Delete the file at the given [offlineMapDirectoryPath].
     *
     * @since 200.8.0
     */
    internal fun deleteContentsForDirectory(
        context: Context,
        offlineMapDirectoryPath: String
    ): Boolean {
        return File(offlineMapDirectoryPath).deleteRecursively()
    }

    /**
     * Removes the offline map information for a given portal item ID.
     * Deletes the info.json and thumbnail file from the corresponding directory
     * and removes the info from the list.
     *
     * @param portalItemID The ID of the portal item whose offline map info should be removed.
     * @since 200.8.0
     */
    internal fun removeOfflineMapInfo(context: Context, portalItemID: String) {
        _offlineMapInfos.removeAll { it.id == portalItemID }
        val baseDir = File(OfflineURLs.offlineRepositoryDirectoryPath(context))
        val offlineMapInfoDir = File(baseDir, portalItemID)
        OfflineMapInfo.removeFromDirectory(offlineMapInfoDir)
    }

    /**
     * Moves [OfflineMapInfo] contents from the pending map info to its final destination.
     *
     * - Moves all info & thumbnail from: `<your-app-files-dir>/OfflineMapAreasCache/PendingMapInfo/<portalItemID>/`
     * - into: `<your-app-files-dir>/com.esri.toolkit.offline/<portalItemID>/`.
     *
     * @since 200.8.0
     */
    private fun moveOfflineMapInfoToDestination(context: Context, portalItemID: String) {
        val pendingDir = File(OfflineURLs.pendingMapInfoDirectoryPath(context, portalItemID))
        val destDir = File(OfflineURLs.portalItemDirectoryPath(context, portalItemID))
        // use pending map info file only if it exists
        val pendingInfoFile = File(pendingDir, offlineMapInfoJsonFile).takeIf { it.exists() }
            ?: return
        val pendingThumbnailFile = File(pendingDir, offlineMapInfoThumbnailFile)
        // null if map info file already exists to prevent overwrite
        val destInfoFile = File(destDir, offlineMapInfoJsonFile).takeIf { !it.exists() }
        val destThumbnailFile = File(destDir, offlineMapInfoThumbnailFile).takeIf { !it.exists() }
        // copy pending map info to destination
        destInfoFile?.let { pendingInfoFile.copyRecursively(it) }
        // copy map info thumbnail if pending thumbnail file exists
        if (pendingThumbnailFile.exists() && destThumbnailFile != null) {
            pendingThumbnailFile.copyRecursively(destThumbnailFile)
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
        context: Context,
        portalItemID: String,
        preplannedMapAreaID: String
    ): String? {
        val destDir = File(
            File(OfflineURLs.portalItemDirectoryPath(context, portalItemID), preplannedMapAreas),
            preplannedMapAreaID
        )
        return if (destDir.exists())
            destDir.path
        else null
    }

    /**
     * Checks whether a given [onDemandMapAreaID] associated with a [portalItemID]
     * has already been downloaded locally.
     *
     * @return The path to the on-demand area’s local folder if it exists,
     *         otherwise `null`.
     * @since 200.8.0
     */
    internal fun isOnDemandAreaDownloaded(
        context: Context,
        portalItemID: String,
        onDemandMapAreaID: String
    ): String? {
        val destDir = File(
            File(OfflineURLs.portalItemDirectoryPath(context, portalItemID), onDemandAreas),
            onDemandMapAreaID
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
        context: Context,
        portalItemId: String,
        mapAreaId: String,
        jsonJobPath: String,
        preplannedMapAreaTitle: String
    ): UUID {
        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<PreplannedMapAreaJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the worker tags
            .addTag(portalItemId)
            .addTag(mapAreaId)
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
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName = jobWorkerUuidKey + workRequest.id,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = workRequest
        )
        return workRequest.id
    }

    /**
     * Creates and enqueues a one-time WorkManager request for downloading an offline map area
     * using [OnDemandMapAreaJobWorker]. Sets up expedited work with input data containing
     * notification and job details. Ensures only one worker instance runs at any time by
     * replacing active workers with the same unique name as per the defined policy.
     *
     * @param jsonJobPath The file path to the serialized JSON representation of the job.
     * @param onDemandMapAreaTitle The title of the on-demand map area being downloaded.
     * @return A [UUID] representing the identifier of the enqueued WorkManager request.
     * @since 200.8.0
     */
    internal fun createOnDemandMapAreaRequestAndQueueDownload(
        context: Context,
        portalItemId: String,
        mapAreaId: String,
        jsonJobPath: String,
        onDemandMapAreaTitle: String
    ): UUID {
        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<OnDemandMapAreaJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the worker tags
            .addTag(portalItemId)
            .addTag(mapAreaId)
            // add the input data
            .setInputData(
                // add the notificationId and the json file path as a key/value pair
                workDataOf(
                    jsonJobPathKey to jsonJobPath,
                    jobAreaTitleKey to onDemandMapAreaTitle
                )
            ).build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        WorkManager.getInstance(context).enqueueUniqueWork(
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
        context: Context,
        offlineWorkerUUID: UUID,
        preplannedMapAreaState: PreplannedMapAreaState,
        portalItem: PortalItem,
        onWorkInfoStateChanged: (WorkInfo) -> Unit,
    ) {
        savePendingMapInfo(context, portalItem)
        preplannedMapAreaState.preplannedMapArea?.let { mapArea ->
            savePendingMapAreaMetadata(
                context = context,
                portalItem = portalItem,
                areaMetadata = OfflineMapAreaMetadata.createPreplannedMetadata(mapArea)
            )
        }
        val workManager = WorkManager.getInstance(context)
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
                            preplannedMapAreaState.updateStatus(PreplannedStatus.Downloaded)
                            workInfo.outputData.getString(mobileMapPackagePathKey)
                                ?.let { mmpkPath ->
                                    // create & load the downloaded map
                                    preplannedMapAreaState.createAndLoadMMPKAndOfflineMap(
                                        mobileMapPackagePath = mmpkPath
                                    )
                                    // skip adding map info if it already exists in the list
                                    if (_offlineMapInfos.find { it.id == portalItem.itemId } == null) {
                                        // create offline map information from local directory
                                        OfflineMapInfo.createFromDirectory(
                                            directory = File(
                                                OfflineURLs.portalItemDirectoryPath(
                                                    context = context,
                                                    portalItemID = portalItem.itemId
                                                )
                                            )
                                        )?.let {
                                            // if non-null info was created, add it to the list
                                            _offlineMapInfos.add(it)
                                        }
                                    }
                                } ?: run {
                                preplannedMapAreaState.updateStatus(
                                    PreplannedStatus.MmpkLoadFailure(
                                        Exception("Mobile Map Package path is null")
                                    )
                                )
                            }
                            preplannedMapAreaState.disposeScope()
                        }
                        // if the work failed or was cancelled
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            preplannedMapAreaState.updateStatus(
                                PreplannedStatus.DownloadFailure(
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
                            preplannedMapAreaState.updateStatus(PreplannedStatus.Downloading)
                        }
                        // don't have to handle other states
                        else -> {}
                    }
                }
            }
    }

    /**
     * Observes updates on a specific WorkManager task identified by its UUID and
     * handles status changes for on-demand map areas. Monitors task states to update
     * corresponding statuses in [OnDemandMapAreasState]. Finally, prunes completed tasks
     * from WorkManager's database when necessary.
     *
     *
     * @param offlineWorkerUUID The unique identifier associated with the specific task being observed in WorkManager.
     * @param onDemandMapAreasState The [onDemandMapAreasState] instance to update based on task progress or completion.
     * @param onWorkInfoStateChanged A callback function triggered when work state changes occur.
     * @since 200.8.0
     */
    internal suspend fun observeStatusForOnDemandWork(
        context: Context,
        offlineWorkerUUID: UUID,
        onDemandMapAreasState: OnDemandMapAreasState,
        portalItem: PortalItem,
        onWorkInfoStateChanged: (WorkInfo) -> Unit,
    ) {
        savePendingMapInfo(context, portalItem)
        onDemandMapAreasState.configuration?.let { mapArea ->
            savePendingMapAreaMetadata(
                context = context,
                portalItem = portalItem,
                areaMetadata = OfflineMapAreaMetadata.createOnDemandMetadata(mapArea)
            )
        }
        val workManager = WorkManager.getInstance(context)
        // collect the flow to get the latest work info list
        workManager.getWorkInfoByIdFlow(offlineWorkerUUID)
            .collect { workInfo ->
                if (workInfo != null) {
                    // Report progress
                    val progress = workInfo.progress.getInt("Progress", 0)
                    onDemandMapAreasState.updateDownloadProgress(progress)

                    // emit changes in the work info state
                    onWorkInfoStateChanged(workInfo)
                    // check the current state of the work request
                    when (workInfo.state) {
                        // if work completed successfully
                        WorkInfo.State.SUCCEEDED -> {
                            onDemandMapAreasState.updateStatus(OnDemandStatus.Downloaded)
                            workInfo.outputData.getString(mobileMapPackagePathKey)
                                ?.let { mmpkPath ->
                                    // create & load the downloaded map
                                    onDemandMapAreasState.createAndLoadMMPKAndOfflineMap(
                                        mobileMapPackagePath = mmpkPath
                                    )
                                    // skip adding map info if it already exists in the list
                                    if (_offlineMapInfos.find { it.id == portalItem.itemId } == null) {
                                        // create offline map information from local directory
                                        OfflineMapInfo.createFromDirectory(
                                            directory = File(
                                                OfflineURLs.portalItemDirectoryPath(
                                                    context = context,
                                                    portalItemID = portalItem.itemId
                                                )
                                            )
                                        )?.let {
                                            // if non-null info was created, add it to the list
                                            _offlineMapInfos.add(it)
                                        }
                                    }
                                } ?: run {
                                onDemandMapAreasState.updateStatus(
                                    OnDemandStatus.MmpkLoadFailure(
                                        Exception("Mobile Map Package path is null")
                                    )
                                )
                            }
                            onDemandMapAreasState.disposeScope()
                        }
                        // if the work failed
                        WorkInfo.State.FAILED -> {
                            onDemandMapAreasState.updateStatus(
                                OnDemandStatus.DownloadFailure(
                                    Exception(
                                        "${workInfo.tags}: FAILED. Reason: " +
                                                "${workInfo.outputData.getString("Error")}"
                                    )
                                )
                            )
                            onDemandMapAreasState.disposeScope()
                        }
                        // if the work was cancelled
                        WorkInfo.State.CANCELLED -> {
                            onDemandMapAreasState.updateStatus(OnDemandStatus.DownloadCancelled)
                            onDemandMapAreasState.disposeScope()
                        }
                        // if the work is currently in progress
                        WorkInfo.State.RUNNING -> {
                            onDemandMapAreasState.updateStatus(OnDemandStatus.Downloading)
                        }
                        // don't have to handle other states
                        else -> {

                        }
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
    internal fun cancelWorkRequest(context: Context, workerUUID: UUID) {
        WorkManager.getInstance(context).cancelWorkById(workerUUID)
    }
}
