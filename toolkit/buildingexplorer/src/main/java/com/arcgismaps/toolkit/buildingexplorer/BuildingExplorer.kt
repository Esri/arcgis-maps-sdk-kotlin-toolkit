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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilter
import com.arcgismaps.mapping.layers.buildingscene.BuildingFilterBlock
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSolidFilterMode
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingXrayFilterMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
public class BuildingExplorerState(
    internal val buildingSceneLayer: BuildingSceneLayer?,
    coroutineScope: CoroutineScope
) {
    // the visibility of the building layer
    internal var visible by mutableStateOf(true)

    // whether or not the full model is being shown
    internal var showFullModel by mutableStateOf(false)

    // The selected level
    public var selectedLevel: String by mutableStateOf("All")

    // The list of available levels
    public val levels: MutableList<String> = mutableStateListOf(selectedLevel)

    // the index of the selected construction phase
    internal var selectedConstructionPhaseIndex by mutableIntStateOf(0)

    // the list of construction phases
    internal val constructionPhases: MutableList<String> = mutableStateListOf()

    // construction phase elements should only show if there are more than 2 phases
    internal val isShowConstructionState by derivedStateOf { constructionPhases.size > 2 }

    // The list of building sublayer categories
    public val categories: MutableList<BuildingSublayer> = mutableStateListOf()

    private var overviewSublayer: BuildingSublayer? = null
    private var fullModelSublayer: BuildingSublayer? = null

    // the show full model switch should only appear if both the full model and overview sublayers
    // are available
    internal var isShowFullModelSwitch by mutableStateOf(false)

    init {
        coroutineScope.launch {
            buildingSceneLayer?.let { buildingSceneLayer ->
                // load the layer and extract the overview and full model sublayers if they are avaialble
                buildingSceneLayer.load().onFailure { throw it }

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
                        levels.addAll(0, it.sorted())
                    }
                    statistics["CreatedPhase"]?.mostFrequentValues?.let {
                        constructionPhases.addAll(0, it.sorted())
                        selectedConstructionPhaseIndex = constructionPhases.size - 1
                    }

                    // The top-level sublayer groups will be the categories
                    fullModelSublayer?.let { buildingSublayer ->
                        buildingSublayer as BuildingGroupSublayer
                        categories.addAll(buildingSublayer.sublayers.sortedBy { it.name })
                    }
                }
            }
        }
    }

    internal fun toggleVisibility(visible: Boolean) {
        this.visible = visible
        buildingSceneLayer?.isVisible = this.visible
    }

    internal fun toggleFullModel(fullModel: Boolean) {
        showFullModel = fullModel
        fullModelSublayer?.isVisible = showFullModel
        overviewSublayer?.isVisible = !showFullModel
    }

    //internal fun zoomToBuilding() {}

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

        if (isShowConstructionState) {
            solidWhere = "CreatedPhase <= ${constructionPhases[selectedConstructionPhaseIndex]}"
            xRayWhere = "CreatedPhase <= ${constructionPhases[selectedConstructionPhaseIndex]}"
        }

        buildingSceneLayer?.let { buildingSceneLayer ->
            //if (selectedLevel == "All") {
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
                Text("Visible")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.visible,
                    onCheckedChange = state::toggleVisibility
                )
            }

            if (state.visible) {
                if (state.isShowFullModelSwitch) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Show full model")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.showFullModel,
                            onCheckedChange = state::toggleFullModel
                        )
                    }
                }
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(8.dp)
//                ) {
//                    Text("Zoom to building")
//                    Spacer(modifier = Modifier.weight(1f))
//                    IconButton(
//                        onClick = state::zoomToBuilding
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.zoom_in_map_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
//                            contentDescription = "Zoom to building"
//                        )
//                    }
//                }

                if (state.showFullModel) {
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
                                value = state.selectedLevel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelsExpanded) },
                                modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = levelsExpanded,
                                onDismissRequest = { levelsExpanded = false }
                            ) {
                                state.levels.forEachIndexed { index, level ->
                                    DropdownMenuItem(
                                        text = { Text(level) },
                                        onClick = {
                                            state.onLevelSelected(index)
                                            levelsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.isShowConstructionState) {
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
                                    value = state.constructionPhases[state.selectedConstructionPhaseIndex],//state.selectedLevel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = constructionPhasesExpanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = constructionPhasesExpanded,
                                    onDismissRequest = { constructionPhasesExpanded = false }
                                ) {
                                    state.constructionPhases.forEachIndexed { index, phase ->
                                        DropdownMenuItem(
                                            text = { Text(phase) },
                                            onClick = {
                                                state.onConstructionPhaseSelected(index)
                                                constructionPhasesExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    CategorySelector(state.categories)
                }
            }
        }
    }
}

/**
 * Check boxes to select building categories and sub-categories
 */
@Composable
private fun CategorySelector(categories: List<BuildingSublayer>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Disciplines and Categories:", modifier = Modifier.padding(8.dp))

        Column {
            categories.forEach { buildingSublayer ->
                var categoryChecked by remember { mutableStateOf(buildingSublayer.isVisible) }
                var showSubCategories by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = buildingSublayer.name, modifier = Modifier.padding(8.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Checkbox(checked = categoryChecked, onCheckedChange = {
                        categoryChecked = it
                        buildingSublayer.isVisible = categoryChecked
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
                    }.forEach {
                        var subCategoryChecked by remember { mutableStateOf(it.isVisible) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = it.name, modifier = Modifier.padding(8.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(checked = subCategoryChecked, onCheckedChange = { isChecked ->
                                subCategoryChecked = isChecked
                                it.isVisible = isChecked
                            })
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun BuildingExplorerPreview() {
    BuildingExplorer(BuildingExplorerState(null, rememberCoroutineScope()))
}

