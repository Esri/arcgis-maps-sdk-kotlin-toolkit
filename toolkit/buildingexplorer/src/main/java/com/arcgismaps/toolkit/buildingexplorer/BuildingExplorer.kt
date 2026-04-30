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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

public class BuildingExplorerState(
    buildingSceneLayer: BuildingSceneLayer?,
    coroutineScope: CoroutineScope
) {
    // The selected floor
    public var selectedLevel: String by mutableStateOf("All")

    // The list of available floors
    public val levels: MutableList<String> = mutableStateListOf(selectedLevel)

    // The list of building sublayer categories
    public val categories: MutableList<BuildingSublayer> = mutableStateListOf()

    init {
        coroutineScope.launch {
            buildingSceneLayer?.let { buildingSceneLayer ->
                buildingSceneLayer.load().onFailure { throw it }
                    .onSuccess { Log.d("BuildingExplorer", "Loaded!!!!!!!!!!") }
                // Get the floor listing from the statistics
                buildingSceneLayer.fetchStatistics().onSuccess { statistics ->
                    statistics["BldgLevel"]?.mostFrequentValues?.let {
                        levels.addAll(0, it.sorted())
                        Log.d("BuildingExplorer", levels.toString())
                    }

                    // The top-level sublayer groups will be the categories
                    buildingSceneLayer.sublayers.find { sublayer ->
                        sublayer.modelName == "FullModel"
                    }?.let { buildingSublayer ->
                        buildingSublayer as BuildingGroupSublayer
                        categories.addAll(buildingSublayer.sublayers.sortedBy { it.name })
                    }
                }
            }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Visible")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = false, onCheckedChange = {})
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Show full model")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = false, onCheckedChange = {})
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Text("Zoom to building")
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.zoom_in_map_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = "Zoom to building"
                    )
                }
            }

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = state.selectedLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.levels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                state.selectedLevel = level
                                expanded = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()
            Text(text = "Disciplines and Categories:", modifier = Modifier.padding(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun BuildingExplorerPreview() {
    BuildingExplorer(BuildingExplorerState(null, rememberCoroutineScope()))
}
