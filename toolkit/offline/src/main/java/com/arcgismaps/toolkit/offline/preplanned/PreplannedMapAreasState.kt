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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedPackagingStatus
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.LOG_TAG
import com.arcgismaps.toolkit.offline.WorkManagerRepository
import com.arcgismaps.toolkit.offline.preplannedMapAreas
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.workmanager.logWorkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlinx.coroutines.cancel

/**
 * Represents the state of a [PreplannedMapArea].
 *
 * @since 200.8.0
 */
internal class PreplannedMapAreaState(
    internal val preplannedMapArea: PreplannedMapArea,
    private val offlineMapTask: OfflineMapTask,
    private val portalItemId: String,
    private val workManagerRepository: WorkManagerRepository,
    private val onSelectionChangedListener: (ArcGISMap) -> Unit
) {
    private lateinit var workerUUID: UUID

    private lateinit var mobileMapPackage: MobileMapPackage
    private lateinit var map: ArcGISMap

    // Enabled when a downloaded map is chosen to be displayed by pressing the "Open" button.
    private var _isSelected by mutableStateOf(false)
    internal val isSelected: Boolean
        get() = _isSelected

    // The status of the preplanned map area.
    private var _status by mutableStateOf<Status>(Status.NotLoaded)
    internal val status: Status
        get() = _status

    // The download progress of the preplanned map area.
    private var _downloadProgress: MutableState<Int> = mutableIntStateOf(0)
    internal val downloadProgress: State<Int> = _downloadProgress

    private lateinit var scope: CoroutineScope

    /**
     * Loads and initializes the associated preplanned map area.
     *
     * This function attempts to load the metadata, packaging status, and thumbnail of the
     * `PreplannedMapArea`. If successful, it updates the current status of the map area.
     * For legacy web maps with incomplete metadata (`Unknown` packaging status), it assumes
     * that a successfully loaded map area is packaged.
     *
     * @return A [Result] indicating success or failure of initialization.
     *
     * @since 200.8.0
     */
    internal suspend fun initialize() = runCatchingCancellable {
        preplannedMapArea.load()
            .onSuccess {
                _status = try {
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
            }
    }

    /**
     * Initiates downloading of the associated preplanned map area for offline use.
     *
     * @since 200.8.0
     */
    internal fun downloadPreplannedMapArea() = runCatchingCancellable {
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            _status = Status.Downloading
            val offlineWorkerUUID = startOfflineMapJob(
                downloadPreplannedOfflineMapJob = createOfflineMapJob(
                    preplannedMapArea = preplannedMapArea
                )
            )
            workManagerRepository.observeStatusForPreplannedWork(
                onWorkInfoStateChanged = ::logWorkInfo,
                preplannedMapAreaState = this@PreplannedMapAreaState,
                offlineWorkerUUID = offlineWorkerUUID
            )
        }
    }

    /**
     * Cancels the current coroutine scope.
     *
     * This function is used to clean up resources and stop any ongoing operations
     * associated with this instance of [PreplannedMapAreaState].
     *
     * @since 200.8.0
     */
    internal fun disposeScope() {
        scope.cancel()
    }

    /**
     * Creates a download job for fetching the preplanned map area offline.
     *
     * Generates default parameters for downloading, including no updates mode and error handling settings.
     * Defines a directory path where map data will be stored and creates a download job using these configurations.
     *
     * @param preplannedMapArea The target [PreplannedMapArea] to be downloaded offline.
     *
     * @return An instance of [DownloadPreplannedOfflineMapJob] configured with download parameters.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflineMapJob(
        preplannedMapArea: PreplannedMapArea
    ): DownloadPreplannedOfflineMapJob {

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
            preplannedMapArea = preplannedMapArea
        ).getOrThrow().apply {
            // Set the update mode to receive no updates
            updateMode = PreplannedUpdateMode.NoUpdates
            continueOnErrors = false
        }

        // Define the path where the map will be saved
        val preplannedMapAreaDownloadDirectory = workManagerRepository.createContentsForPath(
            offlineMapDirectoryName = portalItemId + File.separator + preplannedMapAreas + File.separator + preplannedMapArea.portalItem.itemId
        )

        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
            parameters = params,
            downloadDirectoryPath = preplannedMapAreaDownloadDirectory.path
        )

        return downloadPreplannedOfflineMapJob
    }

    /**
     * Starts an offline map job using WorkManager for managing background tasks.
     *
     * Serializes the download job into JSON format, saves it to disk, and queues it as a WorkRequest
     * in WorkManager.
     *
     * @param downloadPreplannedOfflineMapJob The prepared offline map job to execute using WorkManager.
     *
     * @return A unique identifier ([UUID]) associated with this task within WorkManager's queue system.
     *
     * @since 200.8.0
     */
    private fun startOfflineMapJob(downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob): UUID {
        val jsonJobFile = workManagerRepository.saveJobToDisk(
            jobPath = portalItemId + File.separator + preplannedMapAreas + File.separator + "${preplannedMapArea.portalItem.title}.json",
            jobJson = downloadPreplannedOfflineMapJob.toJson()
        )

        workerUUID = workManagerRepository.createPreplannedMapAreaRequestAndQueDownload(
            notificationId = workManagerRepository.createNotificationIdForJob(),
            jsonJobPath = jsonJobFile.path,
            preplannedMapAreaTitle = preplannedMapArea.portalItem.title
        )

        return workerUUID
    }

    /**
     * Updates the current state of this preplanned map area instance.
     *
     * @param newStatus The updated [Status] value representing this area's current state.
     *
     * @since 200.8.0
     */
    internal fun updateStatus(newStatus: Status) {
        _status = newStatus
    }

    internal fun updateDownloadProgress(progress: Int) {
        _downloadProgress.value = progress
    }

    internal fun cancelDownload() {
        workManagerRepository.cancelWorkRequest(workerUUID)
    }

    internal suspend fun createAndLoadMMPKAndOfflineMap(
        mobileMapPackagePath: String
    ) {
        runCatchingCancellable {
            mobileMapPackage = MobileMapPackage(mobileMapPackagePath)
            mobileMapPackage.load()
                .onSuccess {
                    Log.d(TAG, "Mobile map package loaded successfully")
                }.onFailure { exception ->
                    Log.e(TAG, "Error loading mobile map package", exception)
                    _status = Status.MmpkLoadFailure(exception)
                }
            map = mobileMapPackage.maps.firstOrNull()
                ?: throw IllegalStateException("No maps found in the mobile map package")
        }.onFailure { exception ->
            Log.e(TAG, "Error loading mobile map package", exception)
            _status = Status.MmpkLoadFailure(exception)
        }
    }

    internal fun setSelected(selected: Boolean) {
        _isSelected = selected
        if (selected) {
            onSelectionChangedListener(map)
        }
    }
}

/**
 * Represents various states of a preplanned map area during its lifecycle.
 *
 * @since 200.8.0
 */
internal sealed class Status {

    /**
     * Preplanned map area not loaded.
     */
    data object NotLoaded : Status()

    /**
     * Preplanned map area is loading.
     */
    data object Loading : Status()

    /**
     * Preplanned map area failed to load.
     */
    data class LoadFailure(val error: Throwable) : Status()

    /**
     * Preplanned map area is packaging.
     */
    data object Packaging : Status()

    /**
     * Preplanned map area is packaged and ready for download.
     */
    data object Packaged : Status()

    /**
     * Preplanned map area packaging failed.
     */
    data object PackageFailure : Status()

    /**
     * Preplanned map area is being downloaded.
     */
    data object Downloading : Status()

    /**
     * Preplanned map area is downloaded.
     */
    data object Downloaded : Status()

    /**
     * Preplanned map area failed to download.
     */
    data class DownloadFailure(val error: Throwable) : Status()

    /**
     * Downloaded mobile map package failed to load.
     */
    data class MmpkLoadFailure(val error: Throwable) : Status()

    companion object {
        /**
         * Maps a given packaging status to the corresponding [Status] type.
         *
         * @param packagingStatus The packaging status to translate into a [Status].
         * @return A [Status] object representing the translated state of the preplanned map area.
         * @throws IllegalStateException if the status is unknown or unrecognized.
         *
         * @since 200.8.0
         */
        fun fromPackagingStatus(packagingStatus: PreplannedPackagingStatus): Status {
            return when (packagingStatus) {
                PreplannedPackagingStatus.Processing -> Packaging
                PreplannedPackagingStatus.Failed -> PackageFailure
                PreplannedPackagingStatus.Complete -> Packaged
                PreplannedPackagingStatus.Unknown -> throw IllegalStateException("Unknown packaging status")
            }
        }
    }

    /**
     * Indicates whether the model can load the preplanned map area.
     */
    val canLoadPreplannedMapArea: Boolean
        get() = when (this) {
            is NotLoaded, is LoadFailure, is PackageFailure -> true
            is Loading, is Packaging, is Packaged, is Downloading, is Downloaded, is MmpkLoadFailure, is DownloadFailure -> false
        }

    /**
     * Indicates if download is allowed for this status.
     */
    val allowsDownload: Boolean
        get() = when (this) {
            is Packaged, is DownloadFailure -> true
            is NotLoaded, is Loading, is LoadFailure, is Packaging, is PackageFailure, is Downloading, is Downloaded, is MmpkLoadFailure -> false
        }

    /**
     * Indicates whether the preplanned map area is downloaded.
     */
    val isDownloaded: Boolean
        get() = this is Downloaded
}

private val TAG = LOG_TAG + File.separator + "PreplannedMapAreasState"
