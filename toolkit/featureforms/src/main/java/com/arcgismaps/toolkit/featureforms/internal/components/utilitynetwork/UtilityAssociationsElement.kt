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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult

/**
 * A composable that represents a utility associations element.
 *
 * @param state The [UtilityAssociationsElementState] of the element.
 * @param onItemClick A callback that is called when an item is clicked with the index of the item.
 * The index is the index of the item in the [UtilityAssociationsElementState.filters] list.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationsElement(
    state: UtilityAssociationsElementState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        ElementHeader(
            state.label,
            state.description,
            Modifier.padding(top = 16.dp, end = 16.dp)
        )
        Filters(
            filterResults = state.filters,
            onClick = onItemClick,
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(15.dp))
        )
    }
}

/**
 * Represents the header of a utility associations element.
 *
 * @param label The label of the element.
 * @param description The description of the element.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
private fun ElementHeader(
    label: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Displays the filters for the utility associations element.
 *
 * @param filterResults The list of [UtilityAssociationsFilterResult] to display.
 * @param onClick A callback that is called when a filter is clicked with the index of the filter.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
private fun Filters(
    filterResults: List<UtilityAssociationsFilterResult>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column {
            filterResults.forEachIndexed { i, filterResult ->
                val enabled = filterResult.resultCount > 0
                ListItem(
                    headlineContent = {
                        Text(text = filterResult.filter.title)
                    },
                    modifier = Modifier.clickable(enabled = enabled) {
                        onClick(i)
                    },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = filterResult.resultCount.toString())
                            Image(
                                imageVector = Icons.AutoMirrored.Default.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                if (i < filterResults.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ElementHeaderPreview() {
    ElementHeader("Associations", "This is a description", Modifier.fillMaxWidth())
}
