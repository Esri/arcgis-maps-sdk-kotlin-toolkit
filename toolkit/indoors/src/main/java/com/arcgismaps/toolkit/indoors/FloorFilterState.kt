/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.indoors

import android.view.View
import androidx.compose.material3.Typography
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.floor.FloorFacility
import com.arcgismaps.mapping.floor.FloorLevel
import com.arcgismaps.mapping.floor.FloorManager
import com.arcgismaps.mapping.floor.FloorSite
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the state for the FloorFilter.
 *
 * _Workflow example:_
 *
 * ```
 *  val floorFilterState: FloorFilterState = FloorFilterState(this.map.value, viewModelScope, uiProperties) { floorFilterSelection ->
 *      when (floorFilterSelection.type) {
 *          is FloorFilterSelection.Type.FloorSite -> {
 *              val floorFilterSelectionType =
 *                  floorFilterSelection.type as FloorFilterSelection.Type.FloorSite
 *              floorFilterSelectionType.site.geometry?.let {
 *                  this.setViewpoint(Viewpoint(getEnvelopeWithBuffer(it)))
 *                 }
 *              }
 *              is FloorFilterSelection.Type.FloorFacility -> {
 *              ....
 *
 *       }
 *  }
 * ```
 *
 * @since 200.2.0
 */
public sealed interface FloorFilterState {
    public val floorManager: StateFlow<FloorManager?>
    public val initializationStatus: State<InitializationStatus>
    public val sites: List<FloorSite>
    public val facilities: List<FloorFacility>

    public var selectedSiteId: String?
    public var selectedFacilityId: String?
    public var selectedLevelId: String?

    public val onSiteChanged: StateFlow<FloorSite?>
    public val onFacilityChanged: StateFlow<FloorFacility?>
    public val onLevelChanged: StateFlow<FloorLevel?>

    public val uiProperties: UIProperties

    public suspend fun initialize(): Result<Unit>
    public fun getSelectedSite(): FloorSite?
    public fun getSelectedFacility(): FloorFacility?
}

/**
 * Default implementation for [FloorFilterState].
 *
 * @since 200.2.0
 */
private class FloorFilterStateImpl(
    var geoModel: GeoModel,
    override var uiProperties: UIProperties,
    var onSelectionChangedListener: (FloorFilterSelection) -> Unit
) : FloorFilterState {

    private val _floorManager: MutableStateFlow<FloorManager?> = MutableStateFlow(null)
    override val floorManager: StateFlow<FloorManager?> = _floorManager.asStateFlow()

    private val _onSiteChanged: MutableStateFlow<FloorSite?> = MutableStateFlow(null)
    override val onSiteChanged: StateFlow<FloorSite?> = _onSiteChanged.asStateFlow()

    private val _onFacilityChanged: MutableStateFlow<FloorFacility?> = MutableStateFlow(null)
    override val onFacilityChanged: StateFlow<FloorFacility?> = _onFacilityChanged.asStateFlow()

    private val _onLevelChanged: MutableStateFlow<FloorLevel?> = MutableStateFlow(null)
    override val onLevelChanged: StateFlow<FloorLevel?> = _onLevelChanged.asStateFlow()

    private val _initializationStatus: MutableState<InitializationStatus> =
        mutableStateOf(InitializationStatus.NotInitialized)

    /**
     * The status of the initialization of the state object.
     *
     * @since 200.6.0
     */
    override val initializationStatus: State<InitializationStatus> = _initializationStatus

    /**
     * The list of [FloorLevel]s from the [FloorManager].
     *
     * @since 200.2.0
     */
    private val levels: List<FloorLevel>
        get() {
            return floorManager.value?.levels ?: emptyList()
        }

    /**
     * The list of [FloorSite]s from the [FloorManager].
     *
     * @since 200.2.0
     */
    override val sites: List<FloorSite>
        get() {
            return floorManager.value?.sites ?: emptyList()
        }

    /**
     * The list of [FloorFacility]s from the [FloorManager].
     *
     * @since 200.2.0
     */
    override val facilities: List<FloorFacility>
        get() {
            return floorManager.value?.facilities ?: emptyList()
        }

    /**
     * The selected [FloorSite]'s site ID.
     *
     * @since 200.2.0
     */
    private var _selectedSiteId: String? = null
    override var selectedSiteId: String?
        get() {
            return _selectedSiteId
        }
        set(value) {
            _selectedSiteId = value
            selectedFacilityId = null
            _onSiteChanged.value = getSelectedSite()
            getSelectedSite()?.let {
                onSelectionChangedListener(
                    FloorFilterSelection(
                        FloorFilterSelection.Type.FloorSite(
                            it
                        )
                    )
                )
            }
        }

    /**
     * The selected [FloorFacility]'s facility ID.
     *
     * @since 200.2.0
     */
    private var _selectedFacilityId: String? = null
    override var selectedFacilityId: String?
        get() {
            return _selectedFacilityId
        }
        set(value) {
            _selectedFacilityId = value
            if (_selectedFacilityId != null) {
                _selectedSiteId = getSelectedFacility()?.site?.id
            }
            selectedLevelId = getDefaultLevelIdForFacility(value)
            _onFacilityChanged.value = getSelectedFacility()
            getSelectedFacility()?.let {
                onSelectionChangedListener(
                    FloorFilterSelection(
                        FloorFilterSelection.Type.FloorFacility(
                            it
                        )
                    )
                )
            }
        }

    /**
     * The selected [FloorLevel]'s level ID.
     *
     * @since 200.2.0
     */
    private var _selectedLevelId: String? = null
    override var selectedLevelId: String?
        get() {
            return _selectedLevelId
        }
        set(value) {
            if (_selectedLevelId != value) {
                _selectedLevelId = value
                if (_selectedLevelId != null) {
                    val selectedLevelsFacility = getSelectedLevel()?.facility
                    _selectedFacilityId = selectedLevelsFacility?.id
                    _selectedSiteId = selectedLevelsFacility?.site?.id
                }
                filterMap()
            }
            _onFacilityChanged.value = getSelectedFacility()
            _onLevelChanged.value = getSelectedLevel()
            getSelectedLevel()?.let {
                onSelectionChangedListener(
                    FloorFilterSelection(
                        FloorFilterSelection.Type.FloorLevel(
                            it
                        )
                    )
                )
            }
        }

    /**
     * Initializes the state object by loading the GeoModel and the GeoModel's FloorManager.
     *
     * @return the [Result] indicating if the initialization was successful or not
     * @since 200.6.0
     */
    override suspend fun initialize(): Result<Unit> = runCatchingCancellable {
        if (_initializationStatus.value is InitializationStatus.Initialized) {
            return Result.success(Unit)
        }
        _initializationStatus.value = InitializationStatus.Initializing
        geoModel.load().onSuccess {
            val floorManager: FloorManager? = geoModel.floorManager
            if (floorManager == null) {
                val error = FloorFilterException(FloorFilterError.GEOMODEL_HAS_NO_FLOOR_AWARE_DATA)
                _initializationStatus.value = InitializationStatus.FailedToInitialize(error)
                throw error
            }
            floorManager.load().onSuccess {
                _floorManager.value = floorManager
                // make sure the UI gets updated with the values for selectedSiteId, selectedFacilityId
                // and selectedLevelId that were applied before initialization
                setSelectedValuesAppliedBeforeInitialization()
                // no FloorLevel is selected at this point, so clear the FloorFilter from the selected GeoModel
                filterMap()
                _initializationStatus.value = InitializationStatus.Initialized
                return Result.success(Unit)
            }.onFailure {
                _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
                throw it
            }
        }.onFailure {
            _initializationStatus.value = InitializationStatus.FailedToInitialize(it)
            throw it
        }
    }

    /**
     * Sets the selected values that were applied before initialization by the user.
     *
     * @since 200.6.0
     */
    private fun setSelectedValuesAppliedBeforeInitialization() {
        if (_selectedSiteId != null) {
            selectedSiteId = _selectedSiteId
        }
        if (_selectedFacilityId != null) {
            selectedFacilityId = _selectedFacilityId
        }
        if (_selectedLevelId != null) {
            val temp = _selectedLevelId
            _selectedLevelId = null
            selectedLevelId = temp
        }
    }

    /**
     * Returns the selected [FloorLevel] or null if no [FloorLevel] is selected.
     *
     * @since 200.2.0
     */
    fun getSelectedLevel(): FloorLevel? {
        return levels.firstOrNull { isLevelSelected(it) }
    }

    /**
     * Returns the selected [FloorFacility] or null if no [FloorFacility] is selected.
     *
     * @since 200.2.0
     */
    override fun getSelectedFacility(): FloorFacility? {
        return facilities.firstOrNull { isFacilitySelected(it) }
    }

    /**
     * Returns the selected [FloorSite] or null if no [FloorSite] is selected.
     *
     * @since 200.2.0
     */
    override fun getSelectedSite(): FloorSite? {
        return sites.firstOrNull { isSiteSelected(it) }
    }

    /**
     * Returns true if the [level] is selected.
     *
     * @since 200.2.0
     */
    fun isLevelSelected(level: FloorLevel?): Boolean {
        return level != null && selectedLevelId == level.id
    }

    /**
     * Returns true if the [facility] is selected.
     *
     * @since 200.2.0
     */
    fun isFacilitySelected(facility: FloorFacility?): Boolean {
        return facility != null && selectedFacilityId == facility.id
    }

    /**
     * Returns true if the [site] is selected.
     *
     * @since 200.2.0
     */
    fun isSiteSelected(site: FloorSite?): Boolean {
        return site != null && selectedSiteId == site.id
    }

    /**
     * Filters the attached [GeoModel] to the selected [FloorLevel]. If no [FloorLevel] is
     * selected, clears the floor filter from the selected [GeoModel].
     *
     * @since 200.2.0
     */
    private fun filterMap() {
        // Set levels that match the selected level's vertical order to visible
        val selectedLevel = getSelectedLevel()
        floorManager.value?.levels?.forEach {
            it.isVisible = isVisibleWithSelectedLevel(it, selectedLevel)
        }
    }

    /**
     * Returns whether level to check should be visible by checking if it is the selected level
     * (when in the same facility) or if it is the ground floor (vertical order 0) level for
     * all other facilities.
     *
     * @since 200.2.0
     */
    private fun isVisibleWithSelectedLevel(levelToCheck: FloorLevel?, selectedLevel: FloorLevel?): Boolean {
        return when {
            levelToCheck == null -> true
            selectedLevel == null -> levelToCheck.verticalOrder == 0
            levelToCheck.facility == selectedLevel.facility -> levelToCheck == selectedLevel
            else -> levelToCheck.verticalOrder == 0
        }
    }

    /**
     * Returns the level ID of the [FloorLevel] with [FloorLevel.verticalOrder] of 0. If no
     * [FloorLevel] has [FloorLevel.verticalOrder] of 0, it will return the level ID of the
     * [FloorLevel] with the lowest [FloorLevel.verticalOrder].
     *
     * @since 200.2.0
     */
    private fun getDefaultLevelIdForFacility(facilityId: String?): String? {
        val candidateLevels = levels.filter { it.facility?.id == facilityId }
        return (candidateLevels.firstOrNull { it.verticalOrder == 0 } ?: getLowestLevel(
            candidateLevels
        ))?.id
    }

    /**
     * Returns the [FloorLevel] with the lowest[FloorLevel.verticalOrder].
     *
     * @since 200.2.0
     */
    private fun getLowestLevel(levels: List<FloorLevel>): FloorLevel? {
        var lowestLevel: FloorLevel? = null
        levels.forEach {
            if (it.verticalOrder != Int.MIN_VALUE && it.verticalOrder != Int.MAX_VALUE) {
                val lowestVerticalOrder = lowestLevel?.verticalOrder
                if (lowestVerticalOrder == null || lowestVerticalOrder > it.verticalOrder) {
                    lowestLevel = it
                }
            }
        }
        return lowestLevel
    }
}

/**
 * Represents the position of the level list close button.
 *
 * @since 200.2.0
 */
public enum class ButtonPosition {
    /**
     * The button will be above the level list.
     *
     * @since 200.2.0
     */
    Top,

    /**
     * The button will be below the level list.
     *
     * @since 200.2.0
     */
    Bottom
}

/**
 * Factory function for the creating FloorFilterState.
 *
 * @param geoModel the floor aware geoModel that drives the [FloorFilter]
 * @param coroutineScope scope for [FloorFilterState] that it can use to load the [GeoModel] and the
 *        FloorManager
 * @param uiProperties set of properties to customize the UI used in the [FloorFilter]
 * @param onSelectionChangedListener a lambda to facilitate setting of new ViewPoint on the [GeoView]
 *        with the Site or Facilities extent whenever a new Site or Facility is selected
 * @since 200.2.0
 */
@Deprecated(
    "Use the factory function without the coroutineScope parameter",
    ReplaceWith("FloorFilterState(geoModel, uiProperties, onSelectionChangedListener)")
)
public fun FloorFilterState(
    geoModel: GeoModel,
    coroutineScope: CoroutineScope,
    uiProperties: UIProperties = UIProperties(),
    onSelectionChangedListener: (FloorFilterSelection) -> Unit = { }
): FloorFilterState =
    FloorFilterStateImpl(geoModel, uiProperties, onSelectionChangedListener)

/**
 * Factory function for the creating FloorFilterState.
 *
 * @param geoModel the floor aware geoModel that drives the [FloorFilter]
 * @param uiProperties set of properties to customize the UI used in the [FloorFilter]
 * @param onSelectionChangedListener a lambda to facilitate setting of new ViewPoint on the [GeoView]
 *        with the Site or Facilities extent whenever a new Site or Facility is selected
 * @since 200.6.0
 */
public fun FloorFilterState(
    geoModel: GeoModel,
    uiProperties: UIProperties = UIProperties(),
    onSelectionChangedListener: (FloorFilterSelection) -> Unit = { }
): FloorFilterState =
    FloorFilterStateImpl(geoModel, uiProperties, onSelectionChangedListener)

/**
 * Represents the status of the initialization of the state object.
 *
 * @since 200.6.0
 */
public sealed class InitializationStatus {
    /**
     * The state object is initialized and ready to use.
     *
     * @since 200.6.0
     */
    public data object Initialized : InitializationStatus()

    /**
     * The state object is initializing.
     *
     * @since 200.6.0
     */
    public data object Initializing : InitializationStatus()

    /**
     * The state object is not initialized.
     *
     * @since 200.6.0
     */
    public data object NotInitialized : InitializationStatus()

    /**
     * The state object failed to initialize.
     *
     * @since 200.6.0
     */
    public data class FailedToInitialize(val error: Throwable) : InitializationStatus()
}

/**
 * The selection that was made by the user
 *
 * @since 200.2.0
 */
public data class FloorFilterSelection(public val type: Type) {
    /**
     * The type of Selection
     *
     * @since 200.2.0
     */
    public sealed class Type {
        /**
         * The FloorSite
         *
         * @since 200.2.0
         */
        public data class FloorSite(public val site: com.arcgismaps.mapping.floor.FloorSite): Type()
        /**
         * The FloorFacility
         *
         * @since 200.2.0
         */
        public data class FloorFacility(public val facility: com.arcgismaps.mapping.floor.FloorFacility): Type()
        /**
         * The FloorLevel
         *
         * @since 200.2.0
         */
        public data class FloorLevel(public val level: com.arcgismaps.mapping.floor.FloorLevel): Type()
    }

}

/**
 * UI properties used by the [FloorFilter] component.
 *
 * @since 200.2.0
 */
public data class UIProperties(
    var selectedBackgroundColor: Color = Color(0xFFE2F1FB), // light blue
    var selectedForegroundColor: Color = Color(0xFF005E95), // dark blue
    var searchBackgroundColor: Color = Color(0xFFEEEEEE), // light gray
    var textColor: Color = Color.DarkGray,
    var backgroundColor: Color = Color.White,
    var maxDisplayLevels: Int = -1, // less than 1 will show all of the levels.
    var siteFacilityButtonVisibility: Int = View.VISIBLE,
    var closeButtonVisibility: Int = View.VISIBLE,
    var closeButtonPosition: ButtonPosition = ButtonPosition.Top,
    var buttonSize: Size = Size(60.dp.value, 40.dp.value),
    var typography: Typography = Typography()
)

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
