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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.toolkit.offline.R

/**
 * Displays a list of preplanned map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun PreplannedMapAreas(
    preplannedMapAreas: List<PreplannedMapArea>,
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val preplannedMapAreasSorted = preplannedMapAreas.sortedBy { it.portalItem.title }
        Text(
            text = stringResource(id = R.string.map_areas),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier) {
            items(preplannedMapAreasSorted) { mapArea ->
                Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    mapArea.portalItem.thumbnail?.image?.bitmap?.asImageBitmap()?.let {
                        Image(
                            bitmap = it,
                            contentDescription = stringResource(R.string.thumbnail_description),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(64.dp) // Ensures the image is square
                                .clip(RoundedCornerShape(10.dp)), // Applies rounded corners
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mapArea.portalItem.title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = mapArea.portalItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, // Restrict to two lines
                            overflow = TextOverflow.Ellipsis // Add ellipses if the text overflows
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        onClick = { /* Handle download action here */ }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = stringResource(R.string.download),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (mapArea != preplannedMapAreasSorted.last()) {
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}
