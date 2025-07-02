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

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Item
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedPackagingStatus
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.OfflineMapAreaMetadata
import com.arcgismaps.toolkit.offline.OfflineRepository
import com.arcgismaps.toolkit.offline.internal.utils.getDirectorySize
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.workmanager.LOG_TAG
import com.arcgismaps.toolkit.offline.workmanager.logWorkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Represents the state of a [PreplannedMapArea].
 *
 * @since 200.8.0
 */
internal class PreplannedMapAreaState(
    private val context: Context,
    private val item: Item,
    internal val preplannedMapArea: PreplannedMapArea? = null,
    private val offlineMapTask: OfflineMapTask? = null,
    private val onSelectionChanged: (ArcGISMap) -> Unit
) {

    private lateinit var workerUUID: UUID

    private lateinit var mobileMapPackage: MobileMapPackage
    private lateinit var map: ArcGISMap

    // Enabled when a downloaded map is chosen to be displayed by pressing the "Open" button.
    private var _isSelectedToOpen by mutableStateOf(false)
    internal val isSelectedToOpen: Boolean
        get() = _isSelectedToOpen

    // The status of the preplanned map area.
    private var _status by mutableStateOf<PreplannedStatus>(PreplannedStatus.NotLoaded)
    internal val status: PreplannedStatus
        get() = _status

    // The download progress of the preplanned map area.
    private var _downloadProgress: MutableState<Int> = mutableIntStateOf(0)
    internal val downloadProgress: State<Int> = _downloadProgress

    private var _directorySize by mutableIntStateOf(0)
    internal val directorySize: Int
        get() = _directorySize

    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var _title by mutableStateOf(preplannedMapArea?.portalItem?.title ?: item.title)
    internal val title get() = _title

    private var _description by mutableStateOf(
        preplannedMapArea?.portalItem?.description ?: item.description
    )
    internal val description get() = _description

    private var _thumbnail by mutableStateOf<Bitmap?>(null)
    internal val thumbnail: Bitmap? get() = _thumbnail ?: item.thumbnail?.image?.bitmap

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
        preplannedMapArea?.retryLoad()
            ?.onSuccess {
                _status = try {
                    PreplannedStatus.fromPackagingStatus(preplannedMapArea.packagingStatus)
                } catch (illegalStateException: IllegalStateException) {
                    // Note: Packaging status is `Unknown` for compatibility with legacy webmaps
                    // that have incomplete metadata. We throw an illegalStateException when Package
                    // Status is unknown. We can safely assume that the preplanned map area is packaged.
                    // If the area loads, then we know for certain the status is complete.
                    PreplannedStatus.Packaged
                }
                // Load the thumbnail
                _thumbnail = preplannedMapArea.portalItem.thumbnail?.let { loadableImage ->
                    runCatching { loadableImage.load() }
                    loadableImage.image?.bitmap
                }
            } ?: {
            // preplannedMapArea is null.
        }
    }

    /**
     * Initiates downloading of the associated preplanned map area for offline use.
     *
     * @since 200.8.0
     */
    internal fun downloadPreplannedMapArea() =
        runCatchingCancellable {
            val area = preplannedMapArea ?: return@runCatchingCancellable
            val task = offlineMapTask ?: return@runCatchingCancellable
            val portalItem = item as? PortalItem ?: return@runCatchingCancellable

            if (!scope.isActive)
                scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                _status = PreplannedStatus.Downloading
                val offlineWorkerUUID = startOfflineMapJob(
                    downloadPreplannedOfflineMapJob = createOfflineMapJob(
                        preplannedMapArea = area,
                        offlineMapTask = task
                    ), preplannedMapAreaId = area.portalItem.itemId
                )
                OfflineRepository.observeStatusForPreplannedWork(
                    context = context,
                    onWorkInfoStateChanged = ::logWorkInfo,
                    preplannedMapAreaState = this@PreplannedMapAreaState,
                    portalItem = portalItem,
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
     * @param offlineMapTask The target [OfflineMapTask] to create the params & the job.
     * @return An instance of [DownloadPreplannedOfflineMapJob] configured with download parameters.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflineMapJob(
        preplannedMapArea: PreplannedMapArea,
        offlineMapTask: OfflineMapTask
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
        val preplannedMapAreaDownloadDirectory = OfflineRepository.createPendingPreplannedJobPath(
            context = context,
            portalItemID = item.itemId,
            preplannedMapAreaID = preplannedMapArea.portalItem.itemId
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
     * @param preplannedMapAreaId The map area ID of used to track the job state.
     *
     * @return A unique identifier ([UUID]) associated with this task within WorkManager's queue system.
     *
     * @since 200.8.0
     */
    private fun startOfflineMapJob(
        downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob,
        preplannedMapAreaId: String
    ): UUID {
        val jsonJobFile = OfflineRepository.saveJobToDisk(
            jobPath = downloadPreplannedOfflineMapJob.downloadDirectoryPath,
            jobJson = downloadPreplannedOfflineMapJob.toJson()
        )
        workerUUID = OfflineRepository.createPreplannedMapAreaRequestAndQueueDownload(
            context = context,
            portalItemId = item.itemId,
            mapAreaItemId = preplannedMapAreaId,
            jsonJobPath = jsonJobFile.path,
            preplannedMapAreaTitle = item.title
        )

        return workerUUID
    }

    /**
     * Removes the downloaded preplanned map area from the device.
     *
     * This function deletes the contents of the directory associated with the preplanned map area
     * and updates the status to reflect that the area is no longer loaded. If specified, it also
     * removes the offline map information from the repository.
     *
     * @param shouldRemoveOfflineMapInfo A lambda function that determines whether to remove offline map info.
     *
     * @since 200.8.0
     */
    internal fun removeDownloadedMapArea(shouldRemoveOfflineMapInfo: () -> Boolean) {
        if (OfflineRepository.deleteContentsForDirectory(context, mobileMapPackage.path)) {
            Log.d(TAG, "Deleted preplanned map area: ${mobileMapPackage.path}")
            // Reset the status to reflect the deletion
            _status = PreplannedStatus.NotLoaded
            if (shouldRemoveOfflineMapInfo()) {
                OfflineRepository.removeOfflineMapInfo(
                    context = context,
                    portalItemID = item.itemId
                )
            }
            val localScope = CoroutineScope(Dispatchers.IO)
            localScope.launch {
                initialize()
                localScope.cancel()
            }
        } else {
            Log.e(TAG, "Failed to delete preplanned map area: ${mobileMapPackage.path}")
        }
    }

    /**
     * Updates the current state of this preplanned map area instance.
     *
     * @param newStatus The updated [PreplannedStatus] value representing this area's current state.
     *
     * @since 200.8.0
     */
    internal fun updateStatus(newStatus: PreplannedStatus) {
        _status = newStatus
    }

    internal fun updateDownloadProgress(progress: Int) {
        _downloadProgress.value = progress
    }

    internal fun cancelDownload() {
        OfflineRepository.cancelWorkRequest(context, workerUUID)
    }

    internal suspend fun createAndLoadMMPKAndOfflineMap(
        mobileMapPackagePath: String
    ) {
        runCatchingCancellable {
            mobileMapPackage = MobileMapPackage(mobileMapPackagePath)
            mobileMapPackage.load()
                .onSuccess {
                    _directorySize = getDirectorySize(mobileMapPackagePath)
                    Log.d(TAG, "Mobile map package loaded successfully")
                }.onFailure { exception ->
                    Log.e(TAG, "Error loading mobile map package", exception)
                    _status = PreplannedStatus.MmpkLoadFailure(exception)
                }
            map = mobileMapPackage.maps.firstOrNull()
                ?: throw IllegalStateException("No maps found in the mobile map package")
        }.onFailure { exception ->
            Log.e(TAG, "Error loading mobile map package", exception)
            _status = PreplannedStatus.MmpkLoadFailure(exception)
        }
    }

    internal fun setSelectedToOpen(selected: Boolean) {
        _isSelectedToOpen = selected
        if (selected) {
            onSelectionChanged(map)
        }
    }

    /**
     * Restores and observes the state of a given offline map download job.
     *
     * @since 200.8.0
     */
    fun restoreOfflineMapJobState(
        offlineWorkerUUID: UUID,
        offlineMapAreaMetadata: OfflineMapAreaMetadata
    ) {
        // restore the UI state
        _title = offlineMapAreaMetadata.title
        _description = offlineMapAreaMetadata.description
        _thumbnail = offlineMapAreaMetadata.thumbnailImage
        // observe the active job
        if (!scope.isActive)
            scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            workerUUID = offlineWorkerUUID
            _status = PreplannedStatus.Downloading
            OfflineRepository.observeStatusForPreplannedWork(
                context = context,
                onWorkInfoStateChanged = ::logWorkInfo,
                preplannedMapAreaState = this@PreplannedMapAreaState,
                portalItem = item as PortalItem,
                offlineWorkerUUID = offlineWorkerUUID
            )
        }
    }
}

/**
 * Represents various states of a preplanned map area during its lifecycle.
 *
 * @since 200.8.0
 */
internal sealed class PreplannedStatus {

    /**
     * Preplanned map area not loaded.
     */
    data object NotLoaded : PreplannedStatus()

    /**
     * Preplanned map area is loading.
     */
    data object Loading : PreplannedStatus()

    /**
     * Preplanned map area failed to load.
     */
    data class LoadFailure(val error: Throwable) : PreplannedStatus()

    /**
     * Preplanned map area is packaging.
     */
    data object Packaging : PreplannedStatus()

    /**
     * Preplanned map area is packaged and ready for download.
     */
    data object Packaged : PreplannedStatus()

    /**
     * Preplanned map area packaging failed.
     */
    data object PackageFailure : PreplannedStatus()

    /**
     * Preplanned map area is being downloaded.
     */
    data object Downloading : PreplannedStatus()

    /**
     * Preplanned map area is downloaded.
     */
    data object Downloaded : PreplannedStatus()

    /**
     * Preplanned map area failed to download.
     */
    data class DownloadFailure(val error: Throwable) : PreplannedStatus()

    /**
     * Downloaded mobile map package failed to load.
     */
    data class MmpkLoadFailure(val error: Throwable) : PreplannedStatus()

    companion object {
        /**
         * Maps a given packaging status to the corresponding [PreplannedStatus] type.
         *
         * @param packagingStatus The packaging status to translate into a [PreplannedStatus].
         * @return A [PreplannedStatus] object representing the translated state of the preplanned map area.
         * @throws IllegalStateException if the status is unknown or unrecognized.
         *
         * @since 200.8.0
         */
        fun fromPackagingStatus(packagingStatus: PreplannedPackagingStatus): PreplannedStatus {
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
