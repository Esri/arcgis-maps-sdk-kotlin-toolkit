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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

internal class WorkManagerRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    private val offlineJobJsonPath by lazy {
        createExternalDirPath() + File.separator + jsonJobsTempDir
    }

    internal fun saveJobToDisk(jobPath: String, jobJson: String): File {
        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath + File.separator + jobPath)
        offlineJobJsonFile.parentFile?.mkdirs()
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(jobJson)
        return offlineJobJsonFile
    }

    private fun createExternalDirPath(): String {
        return context.getExternalFilesDir(null)?.path.toString()
    }

    internal fun createContentsForPath(offlineMapDirectoryName: String): File {
        val pathToCreate = createExternalDirPath() + File.separator + offlineMapDirectoryName
        return File(pathToCreate).also { it.mkdirs() }
    }

    internal fun deleteContentsForDirectory(offlineMapDirectoryName: String) {
        val pathToDelete = createExternalDirPath() + File.separator + offlineMapDirectoryName
        File(pathToDelete).deleteRecursively()
    }

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

    internal fun createNotificationIdForJob(): Int {
        return UUID.randomUUID().hashCode()
    }


    /**
     * Starts observing any running or completed OfflineJobWorker work requests by capturing the
     * flow. The flow starts receiving updates when the activity is in started or resumed state.
     * This allows the application to capture immediate progress when in foreground and latest
     * progress when the app resumes or restarts.
     */
    internal suspend fun observeStatusForPreplannedWork(
        onWorkInfoStateChanged: (WorkInfo) -> Unit,
        // TODO, Provide callback lambdas to update status on PreplannedMapAreaState
        preplannedMapAreaState: PreplannedMapAreaState,
        offlineWorkerUUID: UUID
    ) {
        coroutineScope {
            launch {
                // collect the flow to get the latest work info list
                workManager.getWorkInfoByIdFlow(offlineWorkerUUID)
                    .collect { workInfo ->
                        if (workInfo != null) {
                            // emit changes in the work info state
                            onWorkInfoStateChanged(workInfo)
                            // check the current state of the work request
                            when (workInfo.state) {
                                // if work completed successfully
                                WorkInfo.State.SUCCEEDED -> {
                                    preplannedMapAreaState.updateStatus(Status.Downloaded)
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
        }
    }

    internal fun getProgressForUUID(workerUUID: UUID) {

    }
}