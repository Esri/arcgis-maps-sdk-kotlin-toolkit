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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.LocalItem
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreaConfiguration
import com.arcgismaps.toolkit.offline.ondemand.OnDemandMapAreasState
import com.arcgismaps.toolkit.offline.ondemand.OnDemandStatus
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.PreplannedStatus
import com.arcgismaps.toolkit.offline.workmanager.OfflineURLs
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException

/**
 * Represents the state of the offline map.
 *
 * @since 200.8.0
 */
@Stable
public class OfflineMapState(
    private val arcGISMap: ArcGISMap,
    private val onSelectionChanged: (ArcGISMap?) -> Unit = { }
) {
    /**
     * Represents the state of the offline map with a given [OfflineMapInfo].
     *
     * @since 200.8.0
     */
    public constructor(
        offlineMapInfo: OfflineMapInfo,
        onSelectionChanged: (ArcGISMap?) -> Unit = { }
    ) : this(
        arcGISMap = ArcGISMap(offlineMapInfo.portalItemUrl),
        onSelectionChanged = onSelectionChanged
    )

    private var _mode: OfflineMapMode by mutableStateOf(OfflineMapMode.Unknown)
    internal val mode: OfflineMapMode
        get() = _mode

    internal lateinit var localMap: ArcGISMap

    private lateinit var offlineMapTask: OfflineMapTask

    private lateinit var portalItem: PortalItem

    private var _preplannedMapAreaStates: SnapshotStateList<PreplannedMapAreaState> =
        mutableStateListOf()
    internal val preplannedMapAreaStates: List<PreplannedMapAreaState>
        get() = _preplannedMapAreaStates


    private var _onDemandMapAreaStates: SnapshotStateList<OnDemandMapAreasState> =
        mutableStateListOf()
    internal val onDemandMapAreaStates: List<OnDemandMapAreasState>
        get() = _onDemandMapAreaStates

    private val _initializationStatus: MutableState<InitializationStatus> =
        mutableStateOf(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.8.0
     */
    public val initializationStatus: State<InitializationStatus> = _initializationStatus

    /**
     * A Boolean value indicating if only offline models are being shown.
     *
     * @since 200.8.0
     */
    internal var isShowingOnlyOfflineModels by mutableStateOf(false)
        private set

    /**
     * A Boolean value indicating whether the web map is offline disabled.
     *
     * @since 200.8.0
     */
    internal var mapIsOfflineDisabled by mutableStateOf(false)
        private set

    /**
     * Initializes the state object by loading the map, creating and loading the offline map task.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.8.0
     */
    internal suspend fun initialize(context: Context): Result<Unit> = runCatchingCancellable {
        if (_initializationStatus.value is InitializationStatus.Initialized) {
            return Result.success(Unit)
        }
        _initializationStatus.value = InitializationStatus.Initializing
        // initialize the offline repository
        OfflineRepository.refreshOfflineMapInfos(context)
        // reset to check if map has offline enabled
        isShowingOnlyOfflineModels = false
        // load the map, and ignore network error if device is offline
        arcGISMap.retryLoad().getOrElse { error ->
            // check if the error is due to network connection
            if (error is IOException) {
                // enable offline only mode
                isShowingOnlyOfflineModels = true
            } else {
                // unexpected error, report failed status
                _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
                throw error
            }
        }
        localMap = arcGISMap.clone()
        offlineMapTask = OfflineMapTask(arcGISMap)
        portalItem = (arcGISMap.item as? PortalItem)
            ?: throw IllegalStateException("Item not found")

        // load the task, and ignore network error if device is offline
        offlineMapTask.retryLoad().getOrElse { error ->
            // check if the error is not due to network connection
            if (error !is IOException) {
                // unexpected error, report failed status
                _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
                throw error
            }
        }

        // determine if offline is disabled for the map
        mapIsOfflineDisabled =
            (arcGISMap.loadStatus.value == LoadStatus.Loaded) && (arcGISMap.offlineSettings == null)

        // load the preplanned map area states
        loadPreplannedMapAreas(context)

        // check if preplanned for loaded
        if (_mode != OfflineMapMode.Preplanned || _mode == OfflineMapMode.Unknown) {
            loadOfflineOnDemandMapAreas(context)
            if (_mode == OfflineMapMode.Unknown)
                _mode = OfflineMapMode.OnDemand
        }
        // reset the selected map on initialize
        onSelectionChanged(null)
        _initializationStatus.value = InitializationStatus.Initialized
    }

    /**
     * Loads preplanned map areas from the [portalItem], initializes their [preplannedMapAreaStates],
     * and updates download status. If no online areas are available or “offline-only” mode is enabled,
     * falls back [loadOfflinePreplannedMapAreas].
     *
     * @since 200.8.0
     */
    private suspend fun loadPreplannedMapAreas(context: Context) {
        _preplannedMapAreaStates.clear()
        val preplannedMapAreas = mutableListOf<PreplannedMapArea>()
        if (isShowingOnlyOfflineModels) {
            loadOfflinePreplannedMapAreas(context = context)
        } else {
            try {
                preplannedMapAreas.addAll(
                    elements = offlineMapTask.getPreplannedMapAreas().getOrNull() ?: emptyList()
                )
            } catch (e: Exception) {
                preplannedMapAreas.clear()
            }
            preplannedMapAreas.let { preplannedMapArea ->
                preplannedMapArea
                    .sortedBy { it.portalItem.title }
                    .forEach { mapArea ->
                        val preplannedMapAreaState = PreplannedMapAreaState(
                            context = context,
                            preplannedMapArea = mapArea,
                            offlineMapTask = offlineMapTask,
                            item = portalItem,
                            onSelectionChanged = onSelectionChanged
                        )
                        preplannedMapAreaState.initialize()
                        val preplannedPath = OfflineRepository.isPrePlannedAreaDownloaded(
                            context = context,
                            portalItemID = portalItem.itemId,
                            preplannedMapAreaID = mapArea.portalItem.itemId
                        )
                        if (preplannedPath != null) {
                            preplannedMapAreaState.updateStatus(PreplannedStatus.Downloaded)
                            preplannedMapAreaState.createAndLoadMMPKAndOfflineMap(
                                mobileMapPackagePath = preplannedPath
                            )
                        }
                        _mode = OfflineMapMode.Preplanned
                        _preplannedMapAreaStates.add(preplannedMapAreaState)
                    }
                // restore any running download job state
                restoreActiveJobsAndUpdateStates(context)
            }
        }
    }

    /**
     * Scans the local preplanned directory for downloaded maps and creates [PreplannedMapAreaState]s.
     * Sets the [OfflineMapMode.Preplanned] when any local areas are found.
     *
     * @since 200.8.0
     */
    private suspend fun loadOfflinePreplannedMapAreas(context: Context) {
        val preplannedDirectory = File(
            OfflineURLs.prePlannedDirectoryPath(context, portalItem.itemId,null,false)
        )
        val preplannedMapAreaItemIds = preplannedDirectory.listFiles()?.map { it.name.toString() }
            ?: emptyList()

        if (preplannedMapAreaItemIds.isNotEmpty())
            _mode = OfflineMapMode.Preplanned

        preplannedMapAreaItemIds.forEach { itemId ->
            createOfflinePreplannedMapAreaState(context, itemId)
                ?.let { _preplannedMapAreaStates.add(it) }
        }
    }

    /**
     * Scans the local on-demand directory for downloaded maps and creates [OnDemandMapAreasState]s.
     * Sets the [OfflineMapMode.OnDemand] when any local areas are found.
     *
     * @since 200.8.0
     */
    private suspend fun loadOfflineOnDemandMapAreas(context: Context) {
        _onDemandMapAreaStates.clear()
        val onDemandDirectory = File(
            OfflineURLs.onDemandDirectoryPath(context, portalItem.itemId)
        )
        val onDemandMapAreaItemIds = onDemandDirectory.listFiles()?.map { it.name.toString() }
            ?: emptyList()
        if (onDemandMapAreaItemIds.isNotEmpty())
            _mode = OfflineMapMode.OnDemand

        onDemandMapAreaItemIds.forEach { itemId ->
            createOfflineOnDemandMapAreaState(context, itemId)?.let {
                _onDemandMapAreaStates.add(it)
            }
        }
        // restore any running download job state
        restoreActiveJobsAndUpdateStates(context)
    }

    /**
     * Attempts to create a [PreplannedMapAreaState] for a given area ID by loading
     * its [MobileMapPackage] from disk. Returns null if the directory is missing
     * or the package fails to load; otherwise initializes status and map.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflinePreplannedMapAreaState(
        context: Context,
        areaItemId: String
    ): PreplannedMapAreaState? {
        val areaDir = File(
            OfflineURLs.prePlannedDirectoryPath(
                context = context,
                portalItemID = portalItem.itemId,
                preplannedMapAreaID = areaItemId
            )
        )
        if (!areaDir.exists() || !areaDir.isDirectory) return null
        val mmpk = MobileMapPackage(areaDir.absolutePath).apply {
            load().getOrElse { return null }
        }
        val item = (mmpk.item as? LocalItem)?.apply {
            itemId = portalItem.itemId
        } ?: return null

        val preplannedMapAreaState = PreplannedMapAreaState(
            context = context,
            item = item,
            onSelectionChanged = onSelectionChanged
        )
        val preplannedPath = OfflineRepository.isPrePlannedAreaDownloaded(
            context = context,
            portalItemID = portalItem.itemId,
            preplannedMapAreaID = areaItemId
        )
        if (preplannedPath != null) {
            preplannedMapAreaState.updateStatus(PreplannedStatus.Downloaded)
            preplannedMapAreaState.createAndLoadMMPKAndOfflineMap(
                mobileMapPackagePath = preplannedPath
            )
            return preplannedMapAreaState
        } else {
            return null
        }
    }

    /**
     * Attempts to create a [OnDemandMapAreasState] for a given area ID by loading
     * its [MobileMapPackage] from disk. Returns null if the directory is missing
     * or the package fails to load; otherwise initializes status and map.
     *
     * @since 200.8.0
     */
    private suspend fun createOfflineOnDemandMapAreaState(
        context: Context,
        areaItemId: String
    ): OnDemandMapAreasState? {
        val areaDir = File(
            OfflineURLs.onDemandDirectoryPath(
                context = context,
                portalItemID = portalItem.itemId,
                onDemandMapAreaID = areaItemId
            )
        )
        if (!areaDir.exists() || !areaDir.isDirectory) return null
        val mmpk = MobileMapPackage(areaDir.absolutePath).apply {
            load().getOrElse { return null }
        }
        val item = (mmpk.item as? LocalItem)?.apply {
            itemId = portalItem.itemId
        } ?: return null

        val onDemandMapAreasState = OnDemandMapAreasState(
            context = context,
            item = item,
            onSelectionChanged = onSelectionChanged
        )
        val onDemandPath = OfflineRepository.isOnDemandAreaDownloaded(
            context = context,
            portalItemID = portalItem.itemId,
            onDemandMapAreaID = areaItemId
        )
        if (onDemandPath != null) {
            onDemandMapAreasState.updateStatus(OnDemandStatus.Downloaded)
            onDemandMapAreasState.createAndLoadMMPKAndOfflineMap(
                mobileMapPackagePath = onDemandPath
            )
            return onDemandMapAreasState
        } else {
            return null
        }
    }

    /**
     * Creates and adds a new [OnDemandMapAreasState] instance based on the provided [configuration].
     *
     * @since 200.8.0
     */
    internal fun createOnDemandMapAreaState(
        context: Context,
        configuration: OnDemandMapAreaConfiguration
    ): OnDemandMapAreasState {
        val onDemandMapAreasState = OnDemandMapAreasState(
            context = context,
            item = portalItem,
            configuration = configuration,
            offlineMapTask = offlineMapTask,
            onSelectionChanged = onSelectionChanged
        )
        _onDemandMapAreaStates.add(onDemandMapAreasState)
        return onDemandMapAreasState
    }

    /**
     * Resets the current selection of preplanned map areas.
     *
     * @since 200.8.0
     */
    public fun resetSelectedMapArea() {
        _preplannedMapAreaStates.forEach { it.setSelectedToOpen(false) }
        _onDemandMapAreaStates.forEach { it.setSelectedToOpen(false) }
        onSelectionChanged(null)
    }

    /**
     * Support to refresh & re-initialize the offline map area state.
     *
     * @since 200.8.0
     */
    internal fun resetInitialize() {
        _initializationStatus.value = InitializationStatus.NotInitialized
    }

    /**
     * Removes a specific [PreplannedMapAreaState] from the list of preplanned map areas.
     *
     * @since 200.8.0
     */
    internal fun removePreplannedMapArea(state: PreplannedMapAreaState) {
        if (state.isSelectedToOpen) {
            resetSelectedMapArea()
        }
        _preplannedMapAreaStates.remove(state)
    }

    /**
     * Removes a specific [OnDemandMapAreasState] from the list of on-demand map areas.
     *
     * @since 200.8.0
     */
    internal fun removeOnDemandMapArea(state: OnDemandMapAreasState) {
        if (state.isSelectedToOpen) {
            resetSelectedMapArea()
        }
        _onDemandMapAreaStates.remove(state)
    }

    /**
     * Restores the current preplanned & on-demand job state from cached metadata.
     *
     * @since 200.8.0
     */
    private suspend fun restoreActiveJobsAndUpdateStates(context: Context) {
        OfflineRepository.getActiveOfflineJobs(context, portalItem.itemId)
            .forEach { workerUuid ->
                val mapAreaMetadata = OfflineRepository.getMapAreaMetadataForOfflineJob(
                    context = context,
                    uuid = workerUuid,
                    portalItemId = portalItem.itemId
                ) ?: return@forEach
                if (mode == OfflineMapMode.Preplanned) {
                    // update the loaded preplanned area, to restore with the in-progress job state
                    _preplannedMapAreaStates.first {
                        it.preplannedMapArea?.portalItem?.itemId.equals(mapAreaMetadata.itemId)
                    }.apply { restoreOfflineMapJobState(workerUuid, mapAreaMetadata) }
                } else {
                    val restoredState = OnDemandMapAreasState(
                        context = context,
                        item = portalItem,
                        onSelectionChanged = onSelectionChanged
                    ).apply { restoreOfflineMapJobState(workerUuid, mapAreaMetadata) }
                    // add the in-progress job states after loading on-demand map areas
                    _onDemandMapAreaStates.add(restoredState)
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
    runCatching(block).except<CancellationException, R>()
