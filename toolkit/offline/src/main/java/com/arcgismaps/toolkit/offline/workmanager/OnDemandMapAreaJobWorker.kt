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
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


/**
 * Executes a [GenerateOfflineMapJob] as a CoroutineWorker using WorkManager.
 * Manages job execution, progress tracking, foreground notifications, and cleanup operations
 * for a on-demand map download.
 *
 * @since 200.8.0
 */

internal class OnDemandMapAreaJobWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private lateinit var generateOfflineMapJob: GenerateOfflineMapJob

    /**
     * Retrieves the title of the map area being processed from input data.
     */
    private val jobAreaTitle by lazy {
        inputData.getString(key = jobAreaTitleKey) ?: "Unknown area title"
    }

    /**
     * Initializes a [WorkerNotification] instance for managing updates during job execution
     * via notifications such as progress and final status messages.
     */
    private val workerNotification by lazy {
        WorkerNotification(
            applicationContext = context,
            jobAreaTitle = jobAreaTitle,
            workerUuid = id
        )
    }

    /**
     * Provides the [ForegroundInfo] required to run this worker as a foreground service. Sets up
     * an override for API levels below 31 to ensure backward compatibility.
     *
     * @return A [ForegroundInfo] instance configured with progress details.
     * @since 200.8.0
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(progress = 0)
    }

    /**
     * Creates a [ForegroundInfo] object with a [progress] notification based on the given value.
     * Ensures proper setup of the notification ID, type, and visibility depending on the API level.
     *
     * @param progress The current download progress percentage (0-100).
     * @return A [ForegroundInfo] instance configured with ongoing progress details.
     * @since 200.8.0
     */
    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                /* notificationId = */ id.hashCode(),
                /* notification = */ workerNotification.createProgressNotification(progress),
                /* foregroundServiceType = */ FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                /* notificationId = */ id.hashCode(),
                /* notification = */ workerNotification.createProgressNotification(progress)
            )
        }
    }

    /**
     * Performs the main work operation to execute the offline map job. Handles preparation,
     * execution, progress tracking, error management, and resource cleanup during job execution.
     * Implements notifications for user updates.
     *
     * @return A [Result] indicating success or failure of the worker's operation.
     * @since 200.8.0
     */
    override suspend fun doWork(): Result {
        // Retrieve the JSON file path from input data
        val offlineJobJsonPath = inputData.getString(jsonJobPathKey) ?: return Result.failure()
        // Load the JSON file, return failure if it doesn't exist.
        val offlineJobJsonFile = File(offlineJobJsonPath)
        if (!offlineJobJsonFile.exists()) {
            return Result.failure()
        }
        // Deserialize the job from the JSON file; return failure if deserialization fails
        generateOfflineMapJob = GenerateOfflineMapJob.fromJsonOrNull(
            json = offlineJobJsonFile.readText()
        ) ?: return Result.failure()

        return try {
            // Set up this worker to run as a foreground service with initial progress notification.
            setForeground(createForegroundInfo(0))
            // Delete existing map package directory to ensure clean download process.
            File(generateOfflineMapJob.downloadDirectoryPath).deleteRecursively()
            // Start downloading the on-demand offline map job.
            generateOfflineMapJob.start()
            // Collect and handle job progress until completion or failure and get the result
            val jobResult = withContext(Dispatchers.IO) {
                coroutineScope {
                    val progressCollectorJob = launch {
                        // Collect progress until the job has completed in a success/failure
                        generateOfflineMapJob.progress.takeWhile {
                            generateOfflineMapJob.status.value != JobStatus.Failed
                                    && generateOfflineMapJob.status.value != JobStatus.Succeeded
                        }.collect { progress ->
                            // Update the worker progress & ongoing progress notification
                            setProgress(workDataOf("Progress" to progress))
                            setForeground(createForegroundInfo(progress))
                        }
                    }
                    // Suspends until the job has completed
                    val result = generateOfflineMapJob.result()
                    // Cancel the progress collection coroutine if it is still running
                    progressCollectorJob.cancelAndJoin()
                    // Return the job result
                    result
                }
            }
            // Handle success or failure of the job based on its result status.
            if (jobResult.isSuccess) {
                workerNotification.showStatusNotification("The download for $jobAreaTitle has completed successfully.")
                val downloadOnDemandOfflineMapResult = jobResult.getOrNull()
                val outputData = workDataOf(
                    mobileMapPackagePathKey to (downloadOnDemandOfflineMapResult?.mobileMapPackage?.path
                        ?: ""),
                )
                Result.success(outputData)
            } else {
                val errorMessage = jobResult.exceptionOrNull()?.message
                    ?: "Unknown error during job execution"
                Log.e(TAG, "Job failed internally: $errorMessage", jobResult.exceptionOrNull())
                workerNotification.showStatusNotification("The download for $jobAreaTitle failed: $errorMessage")
                Result.failure(workDataOf("Error" to errorMessage))
            }
        } catch (cancellationException: CancellationException) {
            // Handle user/system-triggered cancellation by logging and notifying cancellation status.
            Log.w(TAG, "Job cancelled.", cancellationException)
            workerNotification.showStatusNotification("The download for $jobAreaTitle was cancelled")
            Result.failure(workDataOf("Error" to "Job cancelled by user or system"))
        } catch (exception: Exception) {
            // Log unexpected exceptions and notify failure status with error details.
            Log.e(TAG, "Job failed with exception: ${exception.message}", exception)
            workerNotification.showStatusNotification("The download for $jobAreaTitle failed: ${exception.message}")
            Result.failure(workDataOf("Error" to exception.message))

        } finally {
            withContext(NonCancellable) {
                try {
                    // Cancel ongoing resources and delete temporary files on completion/failure.
                    generateOfflineMapJob.cancel().getOrThrow()
                    offlineJobJsonFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during job cleanup: ${e.message}", e)
                }
            }
        }
    }
}

private val TAG = LOG_TAG + File.separator + "OnDemandAreaJobWorker"
