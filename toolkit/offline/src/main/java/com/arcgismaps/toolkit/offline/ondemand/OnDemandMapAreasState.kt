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
import androidx.core.graphics.drawable.toDrawable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Item
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapUpdateMode
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.toolkit.offline.OfflineMapAreaMetadata
import com.arcgismaps.toolkit.offline.OfflineRepository
import com.arcgismaps.toolkit.offline.internal.utils.ZoomLevel
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
 * A data class to hold configuration for an on-demand map area.
 *
 * @since 200.8.0
 */
internal data class OnDemandMapAreaConfiguration(
    internal val itemId: String,
    internal val title: String,
    internal val minScale: Double,
    internal val maxScale: Double,
    internal val areaOfInterest: Envelope,
    internal val thumbnail: Bitmap?
)

/**
 * Represents the state of a on-demand map area.
 *
 * @since 200.8.0
 */
internal class OnDemandMapAreasState(
    private val context: Context,
    private val item: Item,
    internal val configuration: OnDemandMapAreaConfiguration? = null,
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
    private var _status by mutableStateOf<OnDemandStatus>(OnDemandStatus.NotLoaded)
    internal val status: OnDemandStatus
        get() = _status

    // The download progress of the on-demand map area.
    private var _downloadProgress: MutableState<Int> = mutableIntStateOf(0)
    internal val downloadProgress: State<Int> = _downloadProgress

    private var _directorySize by mutableIntStateOf(0)
    internal val directorySize: Int
        get() = _directorySize

    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var _title by mutableStateOf(configuration?.title ?: item.title)
    internal val title get() = _title

    private var _thumbnail by mutableStateOf(
        configuration?.thumbnail ?: item.thumbnail?.image?.bitmap
    )
    internal val thumbnail: Bitmap? get() = _thumbnail

    /**
     * Initiates downloading of the associated on-demand map area for offline use.
     *
     * @since 200.8.0
     */
    internal fun downloadOnDemandMapArea() = runCatchingCancellable {
        val task = offlineMapTask ?: return@runCatchingCancellable
        val portalItem = item as? PortalItem ?: return@runCatchingCancellable
        val onDemandMapAreaID = configuration?.itemId ?: return@runCatchingCancellable
        val downloadMapArea = configuration.areaOfInterest

        if (!scope.isActive)
            scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            _status = OnDemandStatus.Downloading
            val offlineWorkerUUID = startOfflineMapJob(
                downloadOnDemandOfflineMapJob = createOfflineMapJob(
                    onDemandMapAreaID = onDemandMapAreaID,
                    downloadMapArea = downloadMapArea,
                    offlineMapTask = task
                ), onDemandMapAreaId = onDemandMapAreaID
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
     * @param onDemandMapAreaID The String ID of the map area to download on demand.
     * @param downloadMapArea The target selected map area to be downloaded offline.
     * @param offlineMapTask The target [OfflineMapTask] to create the params & the job.
     * @return An instance of [GenerateOfflineMapJob] configured with download parameters.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflineMapJob(
        onDemandMapAreaID: String,
        downloadMapArea: Envelope,
        offlineMapTask: OfflineMapTask
    ): GenerateOfflineMapJob {

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultGenerateOfflineMapParameters(
            areaOfInterest = downloadMapArea,
            minScale = 0.0,
            maxScale = configuration?.maxScale ?: ZoomLevel.STREET.scale
        ).getOrThrow().apply {
            // Set the update mode to receive no updates
            updateMode = GenerateOfflineMapUpdateMode.NoUpdates
            continueOnErrors = false
            itemInfo?.apply {
                title = this@OnDemandMapAreasState.title
                description = ""
                thumbnail = configuration?.thumbnail?.toDrawable(context.resources)
            }
        }

        // Define the path where the map will be saved
        val onDemandMapAreaDownloadDirectory = OfflineRepository.createPendingOnDemandJobPath(
            context = context,
            portalItemID = item.itemId,
            onDemandMapAreaID = onDemandMapAreaID
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
     * @param onDemandMapAreaId The map area ID of used to track the job state.
     *
     * @return A unique identifier ([UUID]) associated with this task within WorkManager's queue system.
     *
     * @since 200.8.0
     */
    private fun startOfflineMapJob(
        downloadOnDemandOfflineMapJob: GenerateOfflineMapJob,
        onDemandMapAreaId: String
    ): UUID {
        val jsonJobFile = OfflineRepository.saveJobToDisk(
            jobPath = downloadOnDemandOfflineMapJob.downloadDirectoryPath,
            jobJson = downloadOnDemandOfflineMapJob.toJson()
        )

        workerUUID = OfflineRepository.createOnDemandMapAreaRequestAndQueueDownload(
            context = context,
            portalItemId = item.itemId,
            mapAreaItemId = onDemandMapAreaId,
            jsonJobPath = jsonJobFile.path,
            onDemandMapAreaTitle = configuration?.title ?: item.title
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
            _status = OnDemandStatus.NotLoaded
            if (shouldRemoveOfflineMapInfo()) {
                OfflineRepository.removeOfflineMapInfo(
                    context = context,
                    portalItemID = item.itemId
                )
            }
        } else {
            Log.e(TAG, "Failed to delete on-demand map area: ${mobileMapPackage.path}")
        }
    }

    /**
     * Removes the cancelled on-demand map area from the device.
     * If specified, it also removes the offline map information from the repository.
     *
     * @param shouldRemoveOfflineMapInfo A lambda function that determines whether to remove offline map info.
     *
     * @since 200.8.0
     */
    internal fun removeCancelledMapArea(shouldRemoveOfflineMapInfo: () -> Boolean) {
        // Reset the status to reflect the deletion
        _status = OnDemandStatus.NotLoaded
        if (shouldRemoveOfflineMapInfo()) {
            OfflineRepository.removeOfflineMapInfo(
                context = context,
                portalItemID = item.itemId
            )
        }
    }

    /**
     * Updates the current state of this on-demand map area instance.
     *
     * @param newStatus The updated [OnDemandStatus] value representing this area's current state.
     *
     * @since 200.8.0
     */
    internal fun updateStatus(newStatus: OnDemandStatus) {
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
                    _status = OnDemandStatus.MmpkLoadFailure(exception)
                }
            map = mobileMapPackage.maps.firstOrNull()
                ?: throw IllegalStateException("No maps found in the mobile map package")
        }.onFailure { exception ->
            Log.e(TAG, "Error loading mobile map package", exception)
            _status = OnDemandStatus.MmpkLoadFailure(exception)
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
        _thumbnail = offlineMapAreaMetadata.thumbnailImage
        // observe the active job
        if (!scope.isActive)
            scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            workerUUID = offlineWorkerUUID
            _status = OnDemandStatus.Downloading
            OfflineRepository.observeStatusForOnDemandWork(
                context = context,
                onWorkInfoStateChanged = ::logWorkInfo,
                onDemandMapAreasState = this@OnDemandMapAreasState,
                portalItem = item as PortalItem,
                offlineWorkerUUID = offlineWorkerUUID
            )
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
internal sealed class OnDemandStatus {

    /**
     * On-Demand map area not loaded.
     */
    data object NotLoaded : OnDemandStatus()

    /**
     * On-Demand map area is loading.
     */
    data object Loading : OnDemandStatus()

    /**
     * On-Demand map area failed to load.
     */
    data class LoadFailure(val error: Throwable) : OnDemandStatus()

    /**
     * On-Demand map area is packaging.
     */
    data object Packaging : OnDemandStatus()

    /**
     * On-Demand map area is packaged and ready for download.
     */
    data object Packaged : OnDemandStatus()

    /**
     * On-Demand map area packaging failed.
     */
    data object PackageFailure : OnDemandStatus()

    /**
     * On-Demand map area is being downloaded.
     */
    data object Downloading : OnDemandStatus()

    /**
     * On-Demand map area is downloaded.
     */
    data object Downloaded : OnDemandStatus()

    /**
     * On-Demand map area download is cancelled.
     */
    data object DownloadCancelled : OnDemandStatus()

    /**
     * On-Demand map area failed to download.
     */
    data class DownloadFailure(val error: Throwable) : OnDemandStatus()

    /**
     * Downloaded mobile map package failed to load.
     */
    data class MmpkLoadFailure(val error: Throwable) : OnDemandStatus()

    /**
     * Indicates whether the model can load the on-demand map area.
     */
    val canLoadOnDemandMapArea: Boolean
        get() = when (this) {
            is NotLoaded, is LoadFailure, is PackageFailure -> true
            is Loading, is Packaging, is Packaged, is Downloading, is DownloadCancelled, is Downloaded, is MmpkLoadFailure, is DownloadFailure -> false
        }

    /**
     * Indicates if download is allowed for this status.
     */
    val allowsDownload: Boolean
        get() = when (this) {
            is Packaged -> true
            is NotLoaded, is Loading, is LoadFailure, is Packaging, is PackageFailure, is Downloading, is DownloadCancelled, is DownloadFailure, is Downloaded, is MmpkLoadFailure -> false
        }

    /**
     * Indicates whether the on-demand map area is downloaded.
     */
    val isDownloaded: Boolean
        get() = this is Downloaded
}

private val TAG = LOG_TAG + File.separator + "OnDemandMapAreasState"
