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
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.toolkit.offline.jobAreaTitleKey
import com.arcgismaps.toolkit.offline.jobParameter
import com.arcgismaps.toolkit.offline.notificationIdParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Class that runs a [DownloadPreplannedOfflineMapJob] as a CoroutineWorker using WorkManager.
 */
internal class OfflineJobWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private lateinit var downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob

    // notificationId passed by the activity
    private val notificationId by lazy {
        inputData.getInt(key = notificationIdParameter, defaultValue = 1)
    }

    private val jobAreaTitle by lazy {
        inputData.getString(key = jobAreaTitleKey) ?: "Unknown area title"
    }

    // WorkerNotification instance
    private val workerNotification by lazy {
        WorkerNotification(
            applicationContext = context,
            notificationId = notificationId,
            jobAreaTitle = jobAreaTitle
        )
    }

    // must override for api versions < 31 for backwards compatibility
    // with foreground services
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(progress = 0)
    }

    /**
     * Creates and returns a new ForegroundInfo with a progress notification and the given
     * [progress] value.
     */
    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        // create a ForegroundInfo using the notificationId and a new progress notification
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                /* notificationId = */ notificationId,
                /* notification = */ workerNotification.createProgressNotification(progress),
                /* foregroundServiceType = */ FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                /* notificationId = */ notificationId,
                /* notification = */ workerNotification.createProgressNotification(progress)
            )
        }
    }

    override suspend fun doWork(): Result {
        // get the job parameter which is the json file path
        val offlineJobJsonPath = inputData.getString(jobParameter) ?: return Result.failure()
        // load the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // if the file doesn't exist return failure
        if (!offlineJobJsonFile.exists()) {
            return Result.failure()
        }
        // create the DownloadPreplannedOfflineMapJob from the json file
        downloadPreplannedOfflineMapJob = DownloadPreplannedOfflineMapJob.fromJsonOrNull(
            json = offlineJobJsonFile.readText()
        ) ?: return Result.failure() // return failure if the created job is null

        return try {
            // set this worker to run as a long-running foreground service
            // this will throw an exception, if the worker is launched when the app
            // is not in foreground
            setForeground(createForegroundInfo(0))
            // check and delete if the offline map package file already exists
            // this check is needed, if the download has failed midway and is restarted later
            // by WorkManager
            File(downloadPreplannedOfflineMapJob.downloadDirectoryPath).deleteRecursively()

            // start the downloadPreplannedOfflineMapJob
            // this job internally runs on a Dispatchers.IO context, hence this CoroutineWorker
            // can be run on the default Dispatchers.Default context
            downloadPreplannedOfflineMapJob.start()

            // collect job progress, wait for the job to finish and get the result
            val jobResult = coroutineScope {
                // launch the progress collector in a new coroutine
                val progressCollectorJob = launch {
                    // collect on progress until the job has completed in a success/failure
                    downloadPreplannedOfflineMapJob.progress.takeWhile {
                        downloadPreplannedOfflineMapJob.status.value != JobStatus.Failed
                                && downloadPreplannedOfflineMapJob.status.value != JobStatus.Succeeded
                    }.collect { progress ->
                        // update the worker progress
                        setProgress(workDataOf("Progress" to progress))
                        // update the ongoing progress notification
                        setForeground(createForegroundInfo(progress))
                    }
                }
                // suspends until the downloadPreplannedOfflineMapJob has completed
                val result = downloadPreplannedOfflineMapJob.result()
                // cancel the progress collection coroutine if it is still running
                progressCollectorJob.cancelAndJoin()
                // return the result
                result
            }
            // handle and return the result
            if (jobResult.isSuccess) {
                // if the job is successful show a final status notification
                workerNotification.showStatusNotification("The job for $jobAreaTitle has completed successfully.")
                Result.success()
            } else {
                // if the job has failed show a final status notification
                val errorMessage = jobResult.exceptionOrNull()?.message
                    ?: "Unknown error during job execution"
                Log.e(
                    javaClass.simpleName,
                    "Offline map job failed internally: $errorMessage",
                    jobResult.exceptionOrNull()
                )
                workerNotification.showStatusNotification("The job for $jobAreaTitle failed: $errorMessage")
                Result.failure(workDataOf("Error" to errorMessage))
            }
        } catch (cancellationException: CancellationException) {
            // a CancellationException is raised if the work is cancelled manually by the user
            // log and rethrow the cancellationException
            Log.w(
                javaClass.simpleName,
                "Offline map job explicitly cancelled.",
                cancellationException
            )
            workerNotification.showStatusNotification("The job for $jobAreaTitle was cancelled")
            Result.failure(workDataOf("Error" to "Job cancelled by user or system"))
        } catch (exception: Exception) {
            // capture and log if any other exception occurs
            Log.e(
                javaClass.simpleName,
                "Offline map job failed with exception: ${exception.message}",
                exception
            )
            // post a job failed notification
            workerNotification.showStatusNotification("The job for $jobAreaTitle failed: ${exception.message}")
            // return a failure result
            Result.failure(workDataOf("Error" to exception.message))

        } finally {
            withContext(NonCancellable) {
                try {
                    // cancel the job to free up any resources
                    downloadPreplannedOfflineMapJob.cancel().getOrThrow()
                    // delete the json job file
                    if (offlineJobJsonFile.exists()) {
                        offlineJobJsonFile.delete()
                    } else {
                        /*Do nothing*/
                    }

                } catch (e: Exception) {
                    Log.e(
                        javaClass.simpleName,
                        "Error during final cancel of ArcGIS job: ${e.message}",
                        e
                    )
                }
            }
        }
    }
}
