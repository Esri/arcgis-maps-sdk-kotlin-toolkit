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

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    // the index of the selected construction phase
    var selectedConstructionPhaseIndex by mutableIntStateOf(0)
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

                Log.i("Filter", buildingSceneLayer.activeFilter!!.description)

                val sublayers = buildingSceneLayer.sublayers
                overviewSublayer = sublayers.first { it.modelName == "Overview" }
                fullModelSublayer = sublayers.first { it.modelName == "FullModel" }
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
                        selectedConstructionPhaseIndex = constructionPhases.size - 1
                    }

                    // The top-level sublayer groups will be the categories
                    fullModelSublayer?.let { buildingSublayer ->
                        buildingSublayer as BuildingGroupSublayer
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

    internal fun zoomToBuilding() {}

    internal fun onLevelSelected(index: Int) {
        selectedLevel = levels[index]
        filter()
    }

    internal fun onConstructionPhaseSelected(index: Int) {
        selectedConstructionPhaseIndex = index
        filter()
    }

    internal fun filter() {
        var solidWhere = ""
        var xRayWhere = ""

        if (isShowConstructionPhases) {
            solidWhere = "CreatedPhase <= ${constructionPhases[selectedConstructionPhaseIndex]}"
            xRayWhere = "CreatedPhase <= ${constructionPhases[selectedConstructionPhaseIndex]}"
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

public class BuildingExplorerState(
    buildingSceneLayers: PersistentList<BuildingSceneLayer>,
    internal val coroutineScope: CoroutineScope
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
        _buildingSceneLayerState = buildingSceneLayerStates[index]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BuildingExplorer(
    state: BuildingExplorerState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                if (state.buildingSceneLayerStates.size > 1) {
                    var buildingSceneLayerExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = buildingSceneLayerExpanded,
                        onExpandedChange = {
                            buildingSceneLayerExpanded = !buildingSceneLayerExpanded
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        TextField(
                            value = state.buildingSceneLayerState.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = buildingSceneLayerExpanded
                                )
                            },
                            modifier = Modifier.menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = buildingSceneLayerExpanded,
                            onDismissRequest = { buildingSceneLayerExpanded = false }
                        ) {
                            state.buildingSceneLayerStates.forEachIndexed { index, buildingSceneLayerState ->
                                DropdownMenuItem(
                                    text = { Text(buildingSceneLayerState.name) },
                                    onClick = {
                                        state.onBuildingSceneLayerSelected(index)
                                        buildingSceneLayerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(text = state.buildingSceneLayerState.name)
                }
            }
            HorizontalDivider()
            BuildingExplorer(buildingSceneLayerState = state.buildingSceneLayerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildingExplorer(
    buildingSceneLayerState: BuildingSceneLayerState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Visible")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = buildingSceneLayerState.visible,
                    onCheckedChange = buildingSceneLayerState::toggleVisibility
                )
            }

            if (buildingSceneLayerState.visible) {
                if (buildingSceneLayerState.isShowFullModelSwitch) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Show full model")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = buildingSceneLayerState.showFullModel,
                            onCheckedChange = buildingSceneLayerState::toggleFullModel
                        )
                    }
                }

                if (buildingSceneLayerState.showFullModel) {
                    var levelsExpanded by remember { mutableStateOf(false) }
                    Row {
                        Text(text = "Level", modifier = Modifier.padding(8.dp).weight(0.5f))
                        Spacer(modifier = Modifier.weight(0.75f))
                        ExposedDropdownMenuBox(
                            expanded = levelsExpanded,
                            onExpandedChange = { levelsExpanded = !levelsExpanded },
                            modifier = Modifier.padding(8.dp).weight(0.5f)
                        ) {
                            TextField(
                                value = buildingSceneLayerState.selectedLevel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = levelsExpanded
                                    )
                                },
                                modifier = Modifier.menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = levelsExpanded,
                                onDismissRequest = { levelsExpanded = false }
                            ) {
                                buildingSceneLayerState.levels.forEachIndexed { index, level ->
                                    DropdownMenuItem(
                                        text = { Text(level) },
                                        onClick = {
                                            buildingSceneLayerState.onLevelSelected(index)
                                            levelsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (buildingSceneLayerState.isShowConstructionPhases) {
                        var constructionPhasesExpanded by remember { mutableStateOf(false) }
                        Row {
                            Text(
                                text = "Construction phase",
                                modifier = Modifier.padding(8.dp).weight(0.5f)
                            )
                            Spacer(modifier = Modifier.weight(0.75f))
                            ExposedDropdownMenuBox(
                                expanded = constructionPhasesExpanded,
                                onExpandedChange = {
                                    constructionPhasesExpanded = !constructionPhasesExpanded
                                },
                                modifier = Modifier.padding(8.dp).weight(0.5f)
                            ) {
                                TextField(
                                    value = buildingSceneLayerState.constructionPhases[buildingSceneLayerState.selectedConstructionPhaseIndex],
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = constructionPhasesExpanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = constructionPhasesExpanded,
                                    onDismissRequest = { constructionPhasesExpanded = false }
                                ) {
                                    buildingSceneLayerState.constructionPhases.forEachIndexed { index, phase ->
                                        DropdownMenuItem(
                                            text = { Text(phase) },
                                            onClick = {
                                                buildingSceneLayerState.onConstructionPhaseSelected(
                                                    index
                                                )
                                                constructionPhasesExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    buildingSceneLayerState.categories.forEach {
                        key(it) {
                            CategorySelector(
                                buildingSubLayerProvider = { it },
                                onSelected = { buildingSubLayer, isSelected ->
                                    buildingSubLayer.isVisible = isSelected
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelector(
    buildingSubLayerProvider: () -> BuildingSublayer,
    onSelected: (BuildingSublayer, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val buildingSublayer = remember { buildingSubLayerProvider() }
    var categoryChecked by remember { mutableStateOf(buildingSublayer.isVisible) }
    var showSubCategories by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column {
            Row {
                Text(text = buildingSublayer.name, modifier = Modifier.padding(8.dp))
                Spacer(modifier = Modifier.weight(1f))
                Checkbox(checked = categoryChecked, onCheckedChange = {
                    categoryChecked = it
                    onSelected(buildingSublayer, it)
                })
                IconButton(
                    onClick = { showSubCategories = !showSubCategories }
                ) {
                    Icon(
                        imageVector = when {
                            showSubCategories -> Icons.Default.ArrowDropUp
                            else -> Icons.Default.ArrowDropDown
                        },
                        contentDescription = "Show sub-categories",
                        modifier = Modifier
                    )
                }
            }
            if (showSubCategories) {
                remember {
                    val buildingGroupSublayer = buildingSublayer as BuildingGroupSublayer
                    buildingGroupSublayer.sublayers.sortedBy { it.name }
                }.forEach { buildingSublayer ->
                    SubCategorySelector(
                        { buildingSublayer },
                        { buildingSublayer, isChecked -> buildingSublayer.isVisible = isChecked }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubCategorySelector(
    buildingSublayerProvider: () -> BuildingSublayer,
    onSelected: (BuildingSublayer, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val buildingSublayer = remember { buildingSublayerProvider() }
    var subCategoryChecked by remember { mutableStateOf(buildingSublayer.isVisible) }

    Box(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = buildingSublayer.name, modifier = Modifier.padding(8.dp))
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = subCategoryChecked,
                onCheckedChange = { isChecked ->
                    subCategoryChecked = isChecked
                    onSelected(buildingSublayer, isChecked)
                })
        }
    }
}

