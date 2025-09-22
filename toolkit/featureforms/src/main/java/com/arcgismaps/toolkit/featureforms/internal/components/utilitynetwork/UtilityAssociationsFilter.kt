/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult

/**
 * Displays the provided [UtilityAssociationsFilterResult]. The filter result is displayed as a
 * list of its groups as given by [UtilityAssociationsFilterResult.groupResults].
 *
 * @param groupResults The [UtilityAssociationsFilterResult] to display.
 * @param onGroupClick A callback that is called when a group is clicked with the index of the group.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationsFilter(
    groupResults: List<MutableGroupResult>,
    onGroupClick: (MutableGroupResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // show the list of layers
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        if (groupResults.isEmpty()) {
            // No associations found
            Text(
                text = stringResource(R.string.no_associations_found),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Surface
        }
        LazyColumn(modifier = Modifier) {
            groupResults.forEachIndexed { index, group ->
                item {
                    ListItem(
                        headlineContent = {
                            Text(text = group.name, modifier = Modifier.padding(start = 16.dp))
                        },
                        trailingContent = {
                            Text(
                                text = "${group.associationResults.count()}",
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                onGroupClick(group)
                            }
                            .animateItem(),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    if (index < groupResults.count() - 1) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
