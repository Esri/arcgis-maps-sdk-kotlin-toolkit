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

package com.arcgismaps.toolkit.offline.workmanager.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.workmanager.OfflineMapWorkManager
import com.arcgismaps.toolkit.offline.workmanager.WorkerNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "PreplannedDownloadWorker"

internal class PreplannedDownloadWorker(
    context: Context, params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var progressPercentage = 0

    override suspend fun doWork(): Result = coroutineScope {
        // Read input: path to the job JSON file
        val jobJsonPath = inputData.getString(OfflineMapWorkManager.JOB_JSON_KEY)
        if (jobJsonPath.isNullOrEmpty()) {
            Log.e(TAG, "No job JSON path provided")
            return@coroutineScope Result.failure()
        }

        // Create and post initial foreground notification
        WorkerNotification.createNotificationChannel(applicationContext)

        runCatchingCancellable {
            //preplannedDownloadWork(jobJsonPath)
            simulateWork()
        }


        // Complete: send Completed notification and finish
        setForeground(
            WorkerNotification.showCompletionNotification(
                context = applicationContext,
                notificationId = id.hashCode(),
                title = "Offline Map Download",
                message = "Download completed"
            )
        )

        // Return success, passing the download directory path to next worker
        return@coroutineScope Result.success(
            workDataOf(OfflineMapWorkManager.DOWNLOAD_PATH_KEY to "result/mobileMapPackage/path")
        )
    }

    private suspend fun simulateWork() {
        progressPercentage = 0
        while (progressPercentage < 100) {
            Log.d(TAG, "Download progress: $progressPercentage%")
            // Update notification and worker progress
            setForeground(
                WorkerNotification.buildProgressForegroundInfo(
                    context = applicationContext,
                    title = "Offline Map Download",
                    message = "Downloading... $progressPercentage%",
                    notificationId = id.hashCode(),
                    workId = id,
                    progress = progressPercentage
                )
            )
            setProgress(workDataOf(OfflineMapWorkManager.PROGRESS_KEY to progressPercentage))

            delay(100)
            progressPercentage++
        }
    }

    private suspend fun preplannedDownloadWork(jobJsonPath: String): Result {
        // Create the job from JSON
        val json = File(jobJsonPath).bufferedReader().use { it.readText() }
        val offlineMapJob = DownloadPreplannedOfflineMapJob.fromJsonOrNull(json)!!
        Log.d(TAG, "Starting DownloadPreplannedOfflineMapJob")

        // Start job
        val started = offlineMapJob.start()
        if (!started) {
            Log.e(TAG, "Failed to start the offline map job")
            return Result.failure(
                workDataOf(
                    OfflineMapWorkManager.JOB_FAILURE_KEY to "Failed to start the offline map job"
                )
            )
        }

        // Collect progress updates
        coroutineScope {
            launch(Dispatchers.IO) {
                offlineMapJob.progress.collect { progress ->
                    progressPercentage = progress
                    Log.d(TAG, "Download progress: $progressPercentage%")
                    // Update notification and worker progress
                    setForeground(
                        WorkerNotification.buildProgressForegroundInfo(
                            context = applicationContext,
                            title = "Offline Map Download",
                            message = "Downloading... $progressPercentage%",
                            notificationId = id.hashCode(),
                            workId = id,
                            progress = progressPercentage
                        )
                    )
                    setProgress(workDataOf(OfflineMapWorkManager.PROGRESS_KEY to progressPercentage))
                }
            }
        }


        // Await completion
        val downloadPreplannedOfflineMapResult = offlineMapJob.result().getOrElse { throwable ->
            return Result.failure(workDataOf(OfflineMapWorkManager.JOB_FAILURE_KEY to throwable.stackTraceToString()))
        }

        // Check for errors in result
        if (downloadPreplannedOfflineMapResult.hasErrors) {
            return Result.failure(
                workDataOf(
                    OfflineMapWorkManager.JOB_FAILURE_KEY to "Job completed with errors: layers=${downloadPreplannedOfflineMapResult.layerErrors.keys}, tables=${downloadPreplannedOfflineMapResult.tableErrors.keys}"
                )
            )
        }

        Log.d(
            TAG,
            "Download directory: ${downloadPreplannedOfflineMapResult.mobileMapPackage.path}"
        )
        return Result.success(
            workDataOf(
                OfflineMapWorkManager.JOB_SUCCESS_KEY to "Job succeeded without errors",
                OfflineMapWorkManager.DOWNLOAD_PATH_KEY to downloadPreplannedOfflineMapResult.mobileMapPackage.path
            )
        )
    }

    // must override for api versions < 31 for backwards compatibility with foreground services
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return WorkerNotification.buildProgressForegroundInfo(
            context = applicationContext,
            title = "Offline Map Download",
            message = "Downloading... $progressPercentage%",
            notificationId = id.hashCode(),
            workId = id,
            progress = progressPercentage
        )
    }
}
