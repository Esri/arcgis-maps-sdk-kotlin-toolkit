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
import androidx.work.WorkInfo
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.workmanager.OfflineMapWorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Represents the state of the offline map.
 *
 * @since 200.8.0
 */
@Stable
public class OfflineMapState(
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

    private val offlineWorkManager: OfflineMapWorkManager = OfflineMapWorkManager

    /**
     * Initializes the state object by loading the map, creating and loading the offline map task.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.8.0
     */
    internal suspend fun initialize(scope: CoroutineScope, context: Context): Result<Unit> =
        runCatchingCancellable {
            if (_initializationStatus.value is InitializationStatus.Initialized) {
                return Result.success(Unit)
            }
            _initializationStatus.value = InitializationStatus.Initializing
            arcGISMap.load().getOrElse {
                _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
                throw it
            }

            offlineMapTask = OfflineMapTask(arcGISMap)
            portalItemId =
                arcGISMap.item?.itemId ?: throw IllegalStateException("Item ID not found")

            offlineMapTask.load().getOrElse {
                _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
                throw it
            }
            preplannedMapAreas = offlineMapTask.getPreplannedMapAreas().getOrNull()
            if (preplannedMapAreas != null) {
                mode = OfflineMapMode.Preplanned
                // TODO: Update state object to contain scope & app context
                this.scope = scope
                this.context = context
            }

            _initializationStatus.value = InitializationStatus.Initialized
        }

    public suspend fun takePreplannedMapOffline() {
        // The local cache directory for preplanned maps
        val preplannedMapsFolderPath = context.getExternalFilesDir(null)?.path.toString() +
                File.separator + OfflineMapWorkManager.PREPLANNED_MAPS_FOLDER

        // TODO: Wire this worker to run on map area selection
        val preplannedMapAreaToDownload = preplannedMapAreas?.first()!!

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
            preplannedMapArea = preplannedMapAreaToDownload
        ).getOrThrow() // TODO: Propagate error

        // Set the update mode to receive no updates
        params.updateMode = PreplannedUpdateMode.NoUpdates
        // Define the path where the map will be saved
        val downloadDirectoryPath = preplannedMapsFolderPath + File.separator +
                preplannedMapAreaToDownload.portalItem.title
        File(downloadDirectoryPath).mkdirs()
        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob = offlineMapTask
            .createDownloadPreplannedOfflineMapJob(
                parameters = params,
                downloadDirectoryPath = downloadDirectoryPath
            )

        // create a temporary file path to save the offlineMapJob json file
        val offlineJobJsonPath = context.getExternalFilesDir(null)?.path + File.separator +
                OfflineMapWorkManager.JOB_JSON_PREPLANNED
        val workerName = "PreplannedWork:$offlineJobJsonPath"

        // create the json file
        val offlineJobJsonFile = File(offlineJobJsonPath)
        // serialize the offlineMapJob into the file
        offlineJobJsonFile.writeText(downloadPreplannedOfflineMapJob.toJson())
        offlineWorkManager.enqueuePreplannedWork(
            context = context,
            uniqueName = workerName,
            jobJsonPath = offlineJobJsonPath,
        )

        scope.launch {
            offlineWorkManager.getWorkInfosFlow(context, workerName).collect { workInfos ->
                workInfos.forEach { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            Log.e("OfflineMap", "${workInfo.tags}: ENQUEUED")
                        }

                        WorkInfo.State.RUNNING -> {
                            Log.e(
                                "OfflineMap",
                                "${workInfo.tags}: Progress " +
                                        "${
                                            workInfo.progress.getInt(
                                                key = OfflineMapWorkManager.PROGRESS_KEY,
                                                defaultValue = 0
                                            )
                                        }"
                            )
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            Log.e(
                                "OfflineMap",
                                "${workInfo.tags}: SUCCEEDED: ${workInfo.outputData.getString(OfflineMapWorkManager.JOB_SUCCESS_KEY)}"
                            )
                        }

                        WorkInfo.State.FAILED -> {
                            val errorReason =
                                workInfo.outputData.getString(OfflineMapWorkManager.JOB_FAILURE_KEY)
                                    ?: "Unknown failure"
                            Log.e("OfflineMap", "${workInfo.tags}: FAILED: $errorReason")
                        }

                        WorkInfo.State.CANCELLED -> {
                            val errorReason =
                                workInfo.outputData.getString(OfflineMapWorkManager.JOB_CANCELLED_KEY)
                                    ?: "Unknown cancellation"
                            Log.e("OfflineMap", "${workInfo.tags}: CANCELLED: $errorReason")
                        }

                        WorkInfo.State.BLOCKED -> {
                            Log.e("OfflineMap", "${workInfo.tags}: BLOCKED")
                        }
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
internal inline fun <reified T : Throwable, R> Result<R>.except(): Result<R> =
    onFailure { if (it is T) throw it }

/**
 * Runs the specified [block] with [this] value as its receiver and catches any exceptions, returning a `Result` with the
 * result of the block or the exception. If the exception is a [CancellationException], the exception will not be encapsulated
 * in the failure but will be rethrown.
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> =
    runCatching(block)
        .except<CancellationException, R>()
