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

package com.arcgismaps.toolkit.offline

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.workmanager.OfflineJobWorker
import com.arcgismaps.toolkit.offline.workmanager.jobParameter
import com.arcgismaps.toolkit.offline.workmanager.notificationIdParameter
import com.arcgismaps.toolkit.offline.workmanager.uniqueWorkName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/**
 * Represents the state of the offline map.
 *
 * @since 200.8.0
 */
@Stable
public class OfflineMapState (
    private val arcGISMap: ArcGISMap
) {
    internal var mode: OfflineMapMode = OfflineMapMode.Unknown

    private lateinit var offlineMapTask: OfflineMapTask

    private lateinit var portalItemId: String

    internal var preplannedMapAreas: List<PreplannedMapArea>? = null

    private val _initializationStatus: MutableState<InitializationStatus> =
        mutableStateOf(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.8.0
     */
    public val initializationStatus: State<InitializationStatus> = _initializationStatus

    private lateinit var scope: CoroutineScope

    private lateinit var context: Context

    internal val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * Initializes the state object by loading the map, creating and loading the offline map task.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.8.0
     */
    internal suspend fun initialize(scope: CoroutineScope, context: Context): Result<Unit> = runCatchingCancellable {
        if (_initializationStatus.value is InitializationStatus.Initialized) {
            return Result.success(Unit)
        }
        _initializationStatus.value = InitializationStatus.Initializing
        arcGISMap.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }

        offlineMapTask = OfflineMapTask(arcGISMap)
        portalItemId = arcGISMap.item?.itemId ?: throw IllegalStateException("Item ID not found")

        offlineMapTask.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }
        preplannedMapAreas = offlineMapTask.getPreplannedMapAreas().getOrNull()
        if (preplannedMapAreas != null) {
            mode = OfflineMapMode.Preplanned
            // TODO: Wire this worker to run on map area selection
            this.scope = scope
            this.context = context
            takePreplannedMapOffline()
        }

        _initializationStatus.value = InitializationStatus.Initialized
    }
    internal fun takePreplannedMapOffline() {
        val offlineMapPath = context.getExternalFilesDir(null)?.path.toString() +
                File.separator + "PreplannedMapArea"


        val preplannedMapAreaToDownload = preplannedMapAreas?.first()!!

        scope.launch {
            // Create default download parameters from the offline map task
            val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
                preplannedMapArea = preplannedMapAreaToDownload
            ).getOrThrow()
            // Set the update mode to receive no updates
            params.updateMode = PreplannedUpdateMode.NoUpdates
            // Define the path where the map will be saved
            val downloadDirectoryPath = offlineMapPath + File.separator +
                    preplannedMapAreaToDownload.portalItem.title
            File(downloadDirectoryPath).mkdirs()
            // Create a job to download the preplanned offline map
            val downloadPreplannedOfflineMapJob =
                offlineMapTask.createDownloadPreplannedOfflineMapJob(
                    parameters = params, downloadDirectoryPath = downloadDirectoryPath
                )

            // Start observing the worker's progress and status
            observeWorkStatus(workManager, scope, onWorkInfoStateChanged = { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> Log.e("OfflineMap", "ENQUEUED")
                    WorkInfo.State.RUNNING -> Log.e(
                        "OfflineMap", "RUNNING ${workInfo.progress.getInt("Progress", 0)}"
                    )

                    WorkInfo.State.SUCCEEDED -> Log.e("OfflineMap", "SUCCEEDED")
                    WorkInfo.State.FAILED -> {
                        val errorReason =
                            workInfo.outputData.getString("Error") ?: "Unknown failure"
                        Log.e(
                            "OfflineMap",
                            "FAILED: $errorReason - Details: ${workInfo.outputData.keyValueMap}"
                        )
                    }

                    WorkInfo.State.BLOCKED -> Log.e("OfflineMap", "BLOCKED")
                    WorkInfo.State.CANCELLED -> {
                        val errorReason = workInfo.outputData.getString("Error")
                        Log.e(
                            "OfflineMap",
                            "CANCELLED. Reason: $errorReason - Details: ${workInfo.outputData.keyValueMap}"
                        )
                    }
                }
            })
            // Start offline map job
            startOfflineMapJob(workManager, downloadPreplannedOfflineMapJob, context)
        }
    }

    /**
     * Starts the [downloadPreplannedOfflineMapJob] using OfflineJobWorker with WorkManager.
     * The [downloadPreplannedOfflineMapJob] is serialized into a json file and the uri is passed
     * to the OfflineJobWorker, since WorkManager enforces a MAX_DATA_BYTES for the WorkRequest's data
     */
    private fun startOfflineMapJob(
        workManager: WorkManager,
        downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob,
        context: Context
    ) {
        // create a temporary file path to save the offlineMapJob json file
        val offlineJobJsonPath = context.getExternalFilesDir(null)?.path + "/OfflineJobJson"

        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(downloadPreplannedOfflineMapJob.toJson())

        // create a non-zero notification id for the OfflineJobWorker
        // this id will be used to post or update any progress/status notifications
        val notificationId = Random.Default.nextInt(1, 100)

        // create a one-time work request with an instance of OfflineJobWorker
        val workRequest = OneTimeWorkRequestBuilder<OfflineJobWorker>()
            // run it as an expedited work
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // add the input data
            .setInputData(
                // add the notificationId and the json file path as a key/value pair
                workDataOf(
                    notificationIdParameter to notificationId,
                    jobParameter to offlineJobJsonFile.absolutePath
                )
            ).build()


        // enqueue the work request to run as a unique work with the uniqueWorkName, so that
        // only one instance of OfflineJobWorker is running at any time
        // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
        // ones that are active
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
    }

    /**
     * Starts observing any running or completed OfflineJobWorker work requests by capturing the
     * LiveData as a flow. The flow starts receiving updates when the activity is in started
     * or resumed state. This allows the application to capture immediate progress when
     * in foreground and latest progress when the app resumes or restarts.
     */
    private fun observeWorkStatus(
        workManager: WorkManager, scope: CoroutineScope, onWorkInfoStateChanged: (WorkInfo) -> Unit
    ) {
        // get the livedata observer of the unique work as a flow
        val liveDataFlow = workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName).asFlow()

        scope.launch {
            // collect the live data flow to get the latest work info list
            liveDataFlow.collect { workInfoList ->
                if (workInfoList.isNotEmpty()) {
                    // fetch the first work info as we only ever run one work request at any time
                    val workInfo = workInfoList[0]

                    // emit changes in the work info state
                    onWorkInfoStateChanged(workInfo)
                    // check the current state of the work request
                    when (workInfo.state) {
                        // if work completed successfully
                        WorkInfo.State.SUCCEEDED -> {}
                        // if the work failed or was cancelled
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            // this removes the completed WorkInfo from the WorkManager's database
                            // otherwise, the observer will emit the WorkInfo on every launch
                            // until WorkManager auto-prunes
                            workManager.pruneWork()
                        }
                        // if the work is currently in progress
                        WorkInfo.State.RUNNING -> {}
                        // don't have to handle other states
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * Represents the status of the initialization of the state object.
 *
 * @since 200.8.0
 */
public sealed class InitializationStatus {
    /**
     * The state object is initialized and ready to use.
     *
     * @since 200.8.0
     */
    public data object Initialized : InitializationStatus()

    /**
     * The state object is initializing.
     *
     * @since 200.8.0
     */
    public data object Initializing : InitializationStatus()

    /**
     * The state object is not initialized.
     *
     * @since 200.8.0
     */
    public data object NotInitialized : InitializationStatus()

    /**
     * The state object failed to initialize.
     *
     * @since 200.8.0
     */
    public data class FailedToInitialize(val error: Throwable) : InitializationStatus()
}

/**
 * Represents the mode of the offline map.
 *
 * @since 200.8.0
 */
internal enum class OfflineMapMode {
    Preplanned,
    OnDemand,
    Unknown
}

/**
 * Returns [this] Result, but if it is a failure with the specified exception type, then it throws the exception.
 *
 * @param T a [Throwable] type which should be thrown instead of encapsulated in the [Result].
 */
internal inline fun <reified T : Throwable, R> Result<R>.except(): Result<R> = onFailure { if (it is T) throw it }

/**
 * Runs the specified [block] with [this] value as its receiver and catches any exceptions, returning a `Result` with the
 * result of the block or the exception. If the exception is a [CancellationException], the exception will not be encapsulated
 * in the failure but will be rethrown.
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> =
    runCatching(block)
        .except<CancellationException, R>()
