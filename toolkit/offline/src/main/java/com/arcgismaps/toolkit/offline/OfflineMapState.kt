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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

internal const val LOG_TAG = "Offline"
internal const val notificationIdKey = "NotificationId"
internal const val jobAreaTitleKey = "JobAreaTitle"
internal const val jsonJobPathKey = "JsonJobPath"
internal const val prePlannedWorkNameKey = "PreplannedWorker.UUID."
internal const val preplannedMapAreas = "PreplannedMapAreas"
internal const val onDemandAreas = "OnDemandAreas"
internal const val jsonJobsTempDir = "Jobs"
internal const val notificationChannelName = "Offline Map Job Notifications"
internal const val notificationTitle = "Offline Map Download"
internal const val notificationCancelActionKey = "NotificationCancelActionKey"
internal const val notificationChannelDescription =
    "Shows notifications for offline map job progress"

/**
 * Represents the state of the offline map.
 *
 * @since 200.8.0
 */
@Stable
public class OfflineMapState(
    private val arcGISMap: ArcGISMap,
    private val viewModelScope: CoroutineScope,
    private val onSelectionChangedListener: (ArcGISMap) -> Unit = { }
) {
    private lateinit var _workManagerRepository: WorkManagerRepository
    private var _mode: OfflineMapMode = OfflineMapMode.Unknown
    internal val mode: OfflineMapMode
        get() = _mode

    private lateinit var offlineMapTask: OfflineMapTask

    private lateinit var portalItemId: String

    private var _preplannedMapAreaStates: SnapshotStateList<PreplannedMapAreaState> =
        mutableStateListOf()
    internal val preplannedMapAreaStates: List<PreplannedMapAreaState>
        get() = _preplannedMapAreaStates

    private val _initializationStatus: MutableState<InitializationStatus> =
        mutableStateOf(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.8.0
     */
    public val initializationStatus: State<InitializationStatus> = _initializationStatus

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
        arcGISMap.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }

        _workManagerRepository = WorkManagerRepository(context)
        offlineMapTask = OfflineMapTask(arcGISMap)
        portalItemId = arcGISMap.item?.itemId ?: throw IllegalStateException("Item ID not found")

        offlineMapTask.load().getOrElse {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }
        val preplannedMapAreas = offlineMapTask.getPreplannedMapAreas().getOrNull()
        preplannedMapAreas?.let { preplannedMapArea ->
            _mode = OfflineMapMode.Preplanned
            preplannedMapArea
                .sortedBy { it.portalItem.title }
                .forEach {
                    val preplannedMapAreaState = PreplannedMapAreaState(
                        preplannedMapArea = it,
                        offlineMapTask = offlineMapTask,
                        portalItemId = portalItemId,
                        workManagerRepository = _workManagerRepository,
                        onSelectionChangedListener = onSelectionChangedListener
                    )
                    preplannedMapAreaState.initialize()
                    _preplannedMapAreaStates.add(preplannedMapAreaState)
                }
        }
        _initializationStatus.value = InitializationStatus.Initialized
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
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> = runCatching(block).except<CancellationException, R>()
