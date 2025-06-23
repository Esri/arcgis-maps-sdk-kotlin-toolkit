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

package com.arcgismaps.toolkit.offline.ondemand

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Item
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapUpdateMode
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.toolkit.offline.OfflineRepository
import com.arcgismaps.toolkit.offline.internal.utils.CacheScale
import com.arcgismaps.toolkit.offline.internal.utils.getDirectorySize
import com.arcgismaps.toolkit.offline.runCatchingCancellable
import com.arcgismaps.toolkit.offline.workmanager.LOG_TAG
import com.arcgismaps.toolkit.offline.workmanager.logWorkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private data class OnDemandMapAreaConfiguration(
    private val areaID: String,
    private val title: String,
    private val minScale: Double,
    private val maxScale: Double,
    private val areaOfInterest: Envelope,
    private val thumbnail: Bitmap?
)

/**
 * Represents the state of a on-demand map area.
 *
 * @since 200.8.0
 */
internal class OnDemandMapAreasState(
    private val context: Context,
    private val item: Item,
    internal val onDemandAreaID: String,
    internal val title: String,
    private val maxScale: CacheScale = CacheScale.STREET,
    internal val mapAreaEnvelope: Envelope? = null,
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

    // The status of the on-demand map area.
    private var _status by mutableStateOf<Status>(Status.NotLoaded)
    internal val status: Status
        get() = _status

    // The download progress of the on-demand map area.
    private var _downloadProgress: MutableState<Int> = mutableIntStateOf(0)
    internal val downloadProgress: State<Int> = _downloadProgress

    private var _directorySize by mutableIntStateOf(0)
    internal val directorySize: Int
        get() = _directorySize

    private lateinit var scope: CoroutineScope

    private var _thumbnail by mutableStateOf<Bitmap?>(null)
    internal val thumbnail: Bitmap? get() = _thumbnail ?: item.thumbnail?.image?.bitmap

    private var configuration: OnDemandMapAreaConfiguration? = null

    /**
     * Loads and initializes the associated on demand map area.
     *
     * @since 200.8.0
     */
    internal suspend fun initialize() = runCatchingCancellable {
        // TODO
    }

    /**
     * Initiates downloading of the associated on-demand map area for offline use.
     *
     * @since 200.8.0
     */
    internal fun downloadOnDemandMapArea() =
        runCatchingCancellable {
            scope = CoroutineScope(Dispatchers.IO)
            val area = mapAreaEnvelope ?: return@runCatchingCancellable
            val task = offlineMapTask ?: return@runCatchingCancellable
            val portalItem = item as? PortalItem ?: return@runCatchingCancellable

            scope.launch {
                _status = Status.Downloading
                val offlineWorkerUUID = startOfflineMapJob(
                    downloadOnDemandOfflineMapJob = createOfflineMapJob(
                        downloadMapArea = area,
                        offlineMapTask = task
                    )
                )
                OfflineRepository.observeStatusForOnDemandWork(
                    context = context,
                    onWorkInfoStateChanged = ::logWorkInfo,
                    onDemandMapAreasState = this@OnDemandMapAreasState,
                    portalItem = portalItem,
                    offlineWorkerUUID = offlineWorkerUUID
                )
            }
        }

    /**
     * Cancels the current coroutine scope.
     *
     * This function is used to clean up resources and stop any ongoing operations
     * associated with this instance of [OnDemandMapAreasState].
     *
     * @since 200.8.0
     */
    internal fun disposeScope() {
        scope.cancel()
    }

    /**
     * Creates a download job for fetching the on-demand map area offline.
     *
     * Generates default parameters for downloading, including no updates mode and error handling settings.
     * Defines a directory path where map data will be stored and creates a download job using these configurations.
     *
     * @param downloadMapArea The target selected map area to be downloaded offline.
     * @param offlineMapTask The target [OfflineMapTask] to create the params & the job.
     * @return An instance of [GenerateOfflineMapJob] configured with download parameters.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflineMapJob(
        downloadMapArea: Envelope,
        offlineMapTask: OfflineMapTask
    ): GenerateOfflineMapJob {

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultGenerateOfflineMapParameters(
            areaOfInterest = downloadMapArea,
            minScale = 0.0,
            maxScale = maxScale.scale
        ).getOrThrow().apply {
            // Set the update mode to receive no updates
            updateMode = GenerateOfflineMapUpdateMode.NoUpdates
            continueOnErrors = false
            itemInfo?.apply {
                title = this@OnDemandMapAreasState.title
                description = ""
            }
        }

        // Define the path where the map will be saved
        val onDemandMapAreaDownloadDirectory = OfflineRepository.createPendingOnDemandJobPath(
            context = context,
            portalItemID = item.itemId,
            onDemandMapAreaID = onDemandAreaID
        )

        // Create a job to download the on-demand offline map
        val downloadOnDemandOfflineMapJob = offlineMapTask.createGenerateOfflineMapJob(
            parameters = params,
            downloadDirectoryPath = onDemandMapAreaDownloadDirectory.path
        )

        return downloadOnDemandOfflineMapJob
    }

    /**
     * Starts an offline map job using WorkManager for managing background tasks.
     *
     * Serializes the download job into JSON format, saves it to disk, and queues it as a WorkRequest
     * in WorkManager.
     *
     * @param downloadOnDemandOfflineMapJob The on-demand offline map job to execute using WorkManager.
     *
     * @return A unique identifier ([UUID]) associated with this task within WorkManager's queue system.
     *
     * @since 200.8.0
     */
    private fun startOfflineMapJob(downloadOnDemandOfflineMapJob: GenerateOfflineMapJob): UUID {
        val jsonJobFile = OfflineRepository.saveJobToDisk(
            jobPath = downloadOnDemandOfflineMapJob.downloadDirectoryPath,
            jobJson = downloadOnDemandOfflineMapJob.toJson()
        )

        workerUUID = OfflineRepository.createOnDemandMapAreaRequestAndQueueDownload(
            context = context,
            jsonJobPath = jsonJobFile.path,
            onDemandMapAreaTitle = item.title
        )

        return workerUUID
    }

    /**
     * Removes the downloaded on-demand map area from the device.
     *
     * This function deletes the contents of the directory associated with the on-demand map area
     * and updates the status to reflect that the area is no longer loaded. If specified, it also
     * removes the offline map information from the repository.
     *
     * @param shouldRemoveOfflineMapInfo A lambda function that determines whether to remove offline map info.
     *
     * @since 200.8.0
     */
    internal fun removeDownloadedMapArea(shouldRemoveOfflineMapInfo: () -> Boolean) {
        if (OfflineRepository.deleteContentsForDirectory(context, mobileMapPackage.path)) {
            Log.d(TAG, "Deleted on-demand map area: ${mobileMapPackage.path}")
            // Reset the status to reflect the deletion
            _status = Status.NotLoaded
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
            Log.e(TAG, "Failed to delete on-demand map area: ${mobileMapPackage.path}")
        }
    }

    /**
     * Updates the current state of this on-demand map area instance.
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
                    _status = Status.MmpkLoadFailure(exception)
                }
            map = mobileMapPackage.maps.firstOrNull()
                ?: throw IllegalStateException("No maps found in the mobile map package")
        }.onFailure { exception ->
            Log.e(TAG, "Error loading mobile map package", exception)
            _status = Status.MmpkLoadFailure(exception)
        }
    }

    internal fun setSelectedToOpen(selected: Boolean) {
        _isSelectedToOpen = selected
        if (selected) {
            onSelectionChanged(map)
        }
    }
}

/**
 * Represents various states of a on-demand map area during its lifecycle.
 *
 * @since 200.8.0
 */
// TODO: Refine status as not all the status values are being used in onDemand
//  compared to Preplanned in Swift implementation.
internal sealed class Status {

    /**
     * On-Demand map area not loaded.
     */
    data object NotLoaded : Status()

    /**
     * On-Demand map area is loading.
     */
    data object Loading : Status()

    /**
     * On-Demand map area failed to load.
     */
    data class LoadFailure(val error: Throwable) : Status()

    /**
     * On-Demand map area is packaging.
     */
    data object Packaging : Status()

    /**
     * On-Demand map area is packaged and ready for download.
     */
    data object Packaged : Status()

    /**
     * On-Demand map area packaging failed.
     */
    data object PackageFailure : Status()

    /**
     * On-Demand map area is being downloaded.
     */
    data object Downloading : Status()

    /**
     * On-Demand map area is downloaded.
     */
    data object Downloaded : Status()

    /**
     * On-Demand map area failed to download.
     */
    data class DownloadFailure(val error: Throwable) : Status()

    /**
     * Downloaded mobile map package failed to load.
     */
    data class MmpkLoadFailure(val error: Throwable) : Status()

    /**
     * Indicates whether the model can load the on-demand map area.
     */
    val canLoadOnDemandMapArea: Boolean
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
     * Indicates whether the on-demand map area is downloaded.
     */
    val isDownloaded: Boolean
        get() = this is Downloaded
}

private val TAG = LOG_TAG + File.separator + "OnDemandMapAreasState"
