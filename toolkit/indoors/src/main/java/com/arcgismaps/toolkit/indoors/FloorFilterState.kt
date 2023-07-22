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

import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.floor.FloorFacility
import com.arcgismaps.mapping.floor.FloorLevel
import com.arcgismaps.mapping.floor.FloorManager
import com.arcgismaps.mapping.floor.FloorSite
import com.arcgismaps.toolkit.composablemap.MapInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state for the FloorFilter.
 *
 * @since 200.2.0
 */
public sealed interface FloorFilterState {
    public val floorManager: StateFlow<FloorManager?>
    public val sites: List<FloorSite>
    public val facilities: List<FloorFacility>

    public var selectedSiteId: String?
    public var selectedFacilityId: String?
    public var selectedLevelId: String?

    public val onFacilityChanged: StateFlow<FloorFacility?>
    public val onLevelChanged: StateFlow<FloorLevel?>

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
    var coroutineScope: CoroutineScope,
    var onSelectionChangeListener: (FloorFilterSelection) -> Unit
) : FloorFilterState {

    private val _floorManager: MutableStateFlow<FloorManager?> = MutableStateFlow(null)
    override val floorManager: StateFlow<FloorManager?> = _floorManager.asStateFlow()

    private val _onFacilityChanged: MutableStateFlow<FloorFacility?> = MutableStateFlow(null)
    override val onFacilityChanged: StateFlow<FloorFacility?> = _onFacilityChanged.asStateFlow()

    private val _onLevelChanged: MutableStateFlow<FloorLevel?> = MutableStateFlow(null)
    override val onLevelChanged: StateFlow<FloorLevel?> = _onLevelChanged.asStateFlow()

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
            coroutineScope.launch {
                getSelectedSite()?.let {
                    onSelectionChangeListener.invoke(FloorFilterSelection(FloorFilterSelection.Type.FloorSite(it)))
                }
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
            coroutineScope.launch {
                _onFacilityChanged.emit(getSelectedFacility())
                getSelectedFacility()?.let {
                    onSelectionChangeListener.invoke(FloorFilterSelection(FloorFilterSelection.Type.FloorFacility(it)))
                }
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
            coroutineScope.launch {
                _onLevelChanged.emit(getSelectedLevel())
                getSelectedLevel()?.let {
                    onSelectionChangeListener.invoke(FloorFilterSelection(FloorFilterSelection.Type.FloorLevel(it)))
                }
            }
        }

    init {
        coroutineScope.launch {
            loadFloorManager()
        }
    }

    /**
     * Loads the floorManager when the Map is loaded.
     *
     * @since 200.2.0
     */
    private suspend fun loadFloorManager() {
        geoModel.load().onSuccess {
            val floorManager: FloorManager = geoModel.floorManager
                ?: throw IllegalStateException("The map is not configured to be floor aware")
            floorManager.load().onSuccess {
                _floorManager.value = floorManager
            }.onFailure {
                throw it
            }
        }.onFailure {
            throw it
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
        if (selectedLevel == null) {
            clearMapFilter()
        } else {
            floorManager.value?.levels?.forEach {
                it.isVisible = it.verticalOrder == selectedLevel.verticalOrder
            }
        }
    }

    /**
     * Clears the floor filter from the attached [GeoModel].
     *
     * @since 200.2.0
     */
    private fun clearMapFilter() {
        floorManager.value?.levels?.forEach {
            it.isVisible = true
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
 * @since 200.2.0
 */
public fun FloorFilterState(
    geoModel: GeoModel,
    coroutineScope: CoroutineScope,
    onSelectionChangeListener : (FloorFilterSelection) -> Unit
): FloorFilterState =
    FloorFilterStateImpl(geoModel, coroutineScope, onSelectionChangeListener)

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
