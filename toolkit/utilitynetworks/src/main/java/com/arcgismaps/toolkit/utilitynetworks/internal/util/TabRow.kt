/**
 *  Copyright 2024 Esri
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
 */
package com.arcgismaps.toolkit.utilitynetworks.internal.util

import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.arcgismaps.toolkit.utilitynetworks.R

@Composable
internal fun TabRow(
    onTabSelected: () -> Unit,
    tabItems: List<String> = listOf(stringResource(R.string.new_trace), stringResource(R.string.results)),
    selectedIndex: Int
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(selectedIndex) }

    androidx.compose.material3.TabRow(selectedTabIndex = selectedTabIndex) {
        tabItems.forEachIndexed { index, title ->
            Tab(
                selected = index == selectedTabIndex,
                onClick = {
                    if (index != selectedTabIndex) {
                        onTabSelected()
                        selectedTabIndex = index
                    }
                },
                text = { Text(title) }
            )
        }
    }
}
