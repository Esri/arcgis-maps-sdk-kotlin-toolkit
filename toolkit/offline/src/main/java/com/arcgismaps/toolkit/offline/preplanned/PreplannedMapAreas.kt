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

package com.arcgismaps.toolkit.offline.preplanned

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp

/**
 * Displays a list of preplanned map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun PreplannedMapAreas(
    preplannedMapAreas: List<PreplannedMapArea>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(preplannedMapAreas) { mapArea ->
            Text(
                text = mapArea.portalItem.title,
                modifier = Modifier.padding(8.dp)
            )
            HorizontalDivider()
        }
    }
}
