/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.buildingexplorer

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilter
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilterBlock
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSolidFilterMode
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingXrayFilterMode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The state of a [BuildingExplorer].
 *
 * @since 300.2.0
 */
public class BuildingExplorerState(
    buildingSceneLayers: PersistentList<BuildingSceneLayer>,
    coroutineScope: CoroutineScope
) {
    init {
        require(buildingSceneLayers.isNotEmpty()) {
            "BuildingExplorerState requires at least one BuildingSceneLayer."
        }
    }

    internal val buildingSceneLayerStates = buildingSceneLayers.map {
        BuildingSceneLayerState(it, coroutineScope)
    }.sortedBy { it.name }.toPersistentList()

    private var _buildingSceneLayerState by mutableStateOf(
        buildingSceneLayerStates.first()
    )

    internal val buildingSceneLayerState: BuildingSceneLayerState
        get() = _buildingSceneLayerState

    internal fun onBuildingSceneLayerSelected(index: Int) {
        _buildingSceneLayerState = buildingSceneLayerStates.getOrNull(index) ?: return
    }
}

/**
 * The state for a single [BuildingSceneLayer] in a [BuildingExplorerState].
 *
 * @since 300.2.0
 */
internal class BuildingSceneLayerState(
    private val buildingSceneLayer: BuildingSceneLayer,
    coroutineScope: CoroutineScope
) {
    // the name of the building layer
    val name = buildingSceneLayer.name

    // the visibility of the building layer
    var visible by mutableStateOf(true)
        private set

    // whether the full model is being shown
    var showFullModel by mutableStateOf(false)
        private set

    // The selected level
    var selectedLevel: String by mutableStateOf("All")
        private set

    // The list of available levels
    private val _levels = mutableStateListOf(selectedLevel)
    private val levelsState by derivedStateOf { _levels.toList() }
    val levels: List<String> get() = levelsState

    // Levels elements should only show if there are more than 2 levels
    val isShowLevels by derivedStateOf { levels.size > 2 }

    // the index of the selected construction phase
    var selectedConstructionPhase by mutableStateOf("")
        private set

    // the list of construction phases
    //val constructionPhases: MutableList<String> = mutableStateListOf()
    private val _constructionPhases = mutableStateListOf<String>()
    private val constructionPhasesState by derivedStateOf { _constructionPhases.toList() }
    val constructionPhases: List<String> get() = constructionPhasesState

    // construction phase elements should only show if there are more than 2 phases
    val isShowConstructionPhases by derivedStateOf { constructionPhases.size > 2 }

    // The list of building sublayer categories
    private val _categories = mutableStateListOf<BuildingSublayer>()
    private val categoriesState by derivedStateOf { _categories.toList() }
    val categories: List<BuildingSublayer> get() = categoriesState

    private var overviewSublayer: BuildingSublayer? = null
    private var fullModelSublayer: BuildingSublayer? = null

    // the show full model switch should only appear if both the full model and overview sublayers
    // are available
    var isShowFullModelSwitch by mutableStateOf(false)
        private set

    init {
        coroutineScope.launch {
            buildingSceneLayer.let { buildingSceneLayer ->
                // load the layer and extract the overview and full model sublayers if they are available
                buildingSceneLayer.load().onFailure { throw it }

                val sublayers = buildingSceneLayer.sublayers
                overviewSublayer = sublayers.firstOrNull { it.modelName == "Overview" }
                fullModelSublayer = sublayers.firstOrNull { it.modelName == "FullModel" }
                fullModelSublayer?.let {
                    showFullModel = it.isVisible
                }

                isShowFullModelSwitch = fullModelSublayer != null && overviewSublayer != null

                // Get the levels and construction phases from the statistics
                buildingSceneLayer.fetchStatistics().onSuccess { statistics ->
                    statistics["BldgLevel"]?.mostFrequentValues?.let {
                        _levels.addAll(0, it.sortedBy { level -> level.toInt() })
                    }
                    statistics["CreatedPhase"]?.mostFrequentValues?.let {
                        // only allow integer phases because that is what the filter is expecting
                        _constructionPhases.addAll(it.filter { phase -> phase.toIntOrNull() != null }
                            .sortedBy { phase -> phase.toInt() })
                        if (_constructionPhases.isNotEmpty()) {
                            selectedConstructionPhase = _constructionPhases.last()
                        }
                    }

                    // The top-level sublayer groups will be the categories
                    (fullModelSublayer as? BuildingGroupSublayer)?.let { buildingSublayer ->
                        _categories.addAll(buildingSublayer.sublayers.sortedBy { it.name })
                    }
                }

                // clear any preset filter on the layer
                filter()
            }
        }
    }

    internal fun toggleVisibility(visible: Boolean) {
        this.visible = visible
        buildingSceneLayer.isVisible = this.visible
    }

    internal fun toggleFullModel(fullModel: Boolean) {
        showFullModel = fullModel
        fullModelSublayer?.isVisible = showFullModel
        overviewSublayer?.isVisible = !showFullModel
    }

    internal fun onLevelSelected(index: Int) {
        selectedLevel = levels.getOrNull(index) ?: return
        filter()
    }

    internal fun onConstructionPhaseSelected(index: Int) {
        selectedConstructionPhase = constructionPhases.getOrNull(index) ?: return
        filter()
    }

    internal fun filter() {
        var solidWhere = ""
        var xRayWhere = ""

        if (isShowConstructionPhases) {
            solidWhere = "CreatedPhase <= $selectedConstructionPhase"
            xRayWhere = "CreatedPhase <= $selectedConstructionPhase"
        }

        buildingSceneLayer.let { buildingSceneLayer ->
            if (selectedLevel != "All") {
                if (solidWhere.isNotEmpty()) {
                    solidWhere += " AND BldgLevel = $selectedLevel"
                } else {
                    solidWhere = "BldgLevel = $selectedLevel"
                }
                if (xRayWhere.isNotEmpty()) {
                    xRayWhere += " AND BldgLevel < $selectedLevel"
                } else {
                    xRayWhere = "BldgLevel < $selectedLevel"
                }
            }
            // Build a building filter to show the selected floor and an xray view of the floors below.
            // Floors above the selected floor are not shown at all.
            val buildingFilter = BuildingFilter(
                name = "Floor filter",
                description = "Show selected floor and xray filter for lower floors.",
                listOf(
                    BuildingFilterBlock(
                        title = "solid block",
                        whereClause = solidWhere,
                        mode = BuildingSolidFilterMode()
                    ),
                    BuildingFilterBlock(
                        title = "x ray block",
                        whereClause = xRayWhere,
                        mode = BuildingXrayFilterMode()
                    )
                )
            )
            buildingSceneLayer.activeFilter = buildingFilter
        }
    }
}
