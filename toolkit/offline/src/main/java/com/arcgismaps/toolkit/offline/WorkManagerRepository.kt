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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.Status
import com.arcgismaps.toolkit.offline.workmanager.PreplannedMapAreaJobWorker
import java.io.File
import java.util.UUID

/**
 * Manages WorkManager operations for offline map tasks, including job creation, queuing,
 * progress tracking, and cleanup. Provides utilities to handle serialization of jobs,
 * manage notifications, and observe job states for preplanned and onDemand map areas.
 *
 * @since 200.8.0
 */
internal class WorkManagerRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    private val offlineJobJsonPath by lazy {
        createExternalDirPath() + File.separator + jsonJobsTempDir
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
        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath + File.separator + jobPath)
        offlineJobJsonFile.parentFile?.mkdirs()
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(jobJson)
        return offlineJobJsonFile
    }

    /**
     * Retrieves the external directory path for storing application files.
     *
     * @return The external directory path as a [String].
     * @since 200.8.0
     */

    private fun createExternalDirPath(): String {
        return context.getExternalFilesDir(null)?.path.toString()
    }

    /**
     * Creates directories for storing offline map contents at the specified path within external storage.
     *
     * @param offlineMapDirectoryName The name of the directory to create within external storage.
     * @return A [File] instance representing the created directory structure.
     * @since 200.8.0
     */
    internal fun createContentsForPath(offlineMapDirectoryName: String): File {
        val pathToCreate = createExternalDirPath() + File.separator + offlineMapDirectoryName
        return File(pathToCreate).also { it.mkdirs() }
    }

    internal fun deleteContentsForDirectory(offlineMapDirectoryName: String) {
        val pathToDelete = createExternalDirPath() + File.separator + offlineMapDirectoryName
        File(pathToDelete).deleteRecursively()
    }

    /**
     * Creates and enqueues a one-time WorkManager request for downloading an offline map area
     * using [PreplannedMapAreaJobWorker]. Sets up expedited work with input data containing
     * notification and job details. Ensures only one worker instance runs at any time by
     * replacing active workers with the same unique name as per the defined policy.
     *
     * @param notificationId The unique ID for notifications associated with this job.
     * @param jsonJobPath The file path to the serialized JSON representation of the job.
     * @param preplannedMapAreaTitle The title of the preplanned map area being downloaded.
     * @return A [UUID] representing the identifier of the enqueued WorkManager request.
     * @since 200.8.0
     */
    internal fun createPreplannedMapAreaRequestAndQueDownload(
        notificationId: Int,
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
                    notificationIdKey to notificationId,
                    jsonJobPathKey to jsonJobPath,
                    jobAreaTitleKey to preplannedMapAreaTitle
                )
            ).build()

        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        workManager.enqueueUniqueWork(
            uniqueWorkName = prePlannedWorkNameKey + notificationId,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = workRequest
        )
        return workRequest.id
    }

    /**
     * Generates a random unique ID for notifications associated with new jobs.
     *
     * @return An integer representing a randomly generated notification ID.
     * @since 200.8.0
     */
    internal fun createNotificationIdForJob(): Int {
        return UUID.randomUUID().hashCode()
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
        onWorkInfoStateChanged: (WorkInfo) -> Unit,
    ) {
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
                            val path = workInfo.outputData.getString("mobileMapPackagePath")
                            if (path != null) {
                                preplannedMapAreaState.createAndLoadMMPKAndOfflineMap(path)
                            }
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

    internal fun cancelWorkRequest(workerUUID: UUID) {
        workManager.cancelWorkById(workerUUID)
    }
}
