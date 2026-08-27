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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer

/**
 * Building Explorer is a composable for browsing the levels and sublayers of building scene
 * layers.
 *
 * @since 300.2.0
 */
@Composable
public fun BuildingExplorer(
    state: BuildingExplorerState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (state.buildingSceneLayerStates.size > 1) {
                        var buildingSceneLayerExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.padding(8.dp)) {
                            TextField(
                                value = state.buildingSceneLayerState.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (buildingSceneLayerExpanded) {
                                            Icons.Default.ArrowDropUp
                                        } else {
                                            Icons.Default.ArrowDropDown
                                        },
                                        contentDescription = null
                                    )
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { buildingSceneLayerExpanded = true }
                            )

                            DropdownMenu(
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
            }
            item {
                HorizontalDivider()
            }
            item {
                BuildingExplorer(buildingSceneLayerState = state.buildingSceneLayerState)
            }
        }
    }
}

/**
 * Building explorer composable for browsing a single building scene layer.
 *
 * @since 300.2.0
 */
@Composable
private fun BuildingExplorer(
    buildingSceneLayerState: BuildingSceneLayerState,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(stringResource(R.string.visible))
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
                        Text(stringResource(R.string.show_full_model))
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = buildingSceneLayerState.showFullModel,
                            onCheckedChange = buildingSceneLayerState::toggleFullModel
                        )
                    }
                }

                if (buildingSceneLayerState.showFullModel) {
                    if (buildingSceneLayerState.isShowLevels) {
                        var levelsExpanded by remember { mutableStateOf(false) }
                        Row {
                            Text(
                                text = stringResource(R.string.level),
                                modifier = Modifier.padding(8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                TextField(
                                    value = if (buildingSceneLayerState.selectedLevel == "All") {
                                        stringResource(R.string.all)
                                    } else {
                                        buildingSceneLayerState.selectedLevel
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (levelsExpanded) {
                                                Icons.Default.ArrowDropUp
                                            } else {
                                                Icons.Default.ArrowDropDown
                                            },
                                            contentDescription = null
                                        )
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { levelsExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = levelsExpanded,
                                    onDismissRequest = { levelsExpanded = false }
                                ) {
                                    buildingSceneLayerState.levels.forEachIndexed { index, level ->
                                        DropdownMenuItem(
                                            text = {
                                                if (level == "All") {
                                                    Text(stringResource(R.string.all))
                                                } else {
                                                    Text(level)
                                                }
                                            },
                                            onClick = {
                                                buildingSceneLayerState.onLevelSelected(index)
                                                levelsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (buildingSceneLayerState.isShowConstructionPhases) {
                        var constructionPhasesExpanded by remember { mutableStateOf(false) }
                        Row {
                            Text(
                                text = stringResource(R.string.construction_phase),
                                modifier = Modifier.padding(8.dp)
                            )
                            Spacer(modifier = Modifier.weight(/*0.75f*/1f))
                            Box(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                TextField(
                                    value = buildingSceneLayerState.selectedConstructionPhase,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (constructionPhasesExpanded) {
                                                Icons.Default.ArrowDropUp
                                            } else {
                                                Icons.Default.ArrowDropDown
                                            },
                                            contentDescription = null
                                        )
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { constructionPhasesExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = constructionPhasesExpanded,
                                    onDismissRequest = { constructionPhasesExpanded = false }
                                ) {
                                    buildingSceneLayerState.constructionPhases.forEachIndexed { index, phase ->
                                        DropdownMenuItem(
                                            text = { Text(phase) },
                                            onClick = {
                                                buildingSceneLayerState.onConstructionPhaseSelected(index)
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

/**
 * Composable for selecting a building sublayer category e.g. architectural features
 *
 * @since 300.2.0
 */
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
                        contentDescription = "Show sub-categories"
                    )
                }
            }

            val sortedSubCategories = remember(buildingSublayer) {
                (buildingSublayer as? BuildingGroupSublayer)
                    ?.sublayers
                    ?.sortedBy { it.name }
                    .orEmpty()
            }

            if (showSubCategories) {
                sortedSubCategories.forEach {
                    SubCategorySelector(
                        buildingSublayerProvider = { it },
                        onSelected = { it, isChecked ->
                            it.isVisible = isChecked
                        }
                    )
                }
            }
        }
    }
}

/**
 * Composable for selecting building sublayer category subcategories e.g. doors
 *
 * @since 300.2.0
 */
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

