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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TabRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.TraceNavRoute

@Composable
internal fun TabRow(
    selectedIndex: Int,
    onNavigateTo: (Pair<Int, TraceNavRoute>) -> Unit
) {
    val tabItems = listOf(
        stringResource(R.string.new_trace) to TraceNavRoute.TraceOptions,
        stringResource(R.string.results) to TraceNavRoute.TraceResults
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        TabRow(selectedTabIndex = selectedIndex) {
            tabItems.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = {
                        if (index != selectedIndex) {
                            onNavigateTo(index to tab.second)
                        }
                    },
                    text = { Text(tab.first) }
                )
            }
        }

    }
}
