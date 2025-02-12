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

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.objectId
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetworkSource

@Composable
internal fun UNAssociationGroups(
    group: UAFilterResult,
    source: UtilityElement,
    onBackPressed: () -> Unit,
    onGroupClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column {
            Header(
                group.type.name,
                source.objectId.toString(),
                onBackPressed = onBackPressed,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            HorizontalDivider()
            Layers(
                filter = group,
                onClick = onGroupClick,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    BackHandler(onBack = onBackPressed)
}

@Composable
private fun Header(
    label: String,
    source: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
                .size(32.dp)
                .clickable {
                    onBackPressed()
                },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = source,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun Layers(
    filter: UAFilterResult,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // show the list of layers
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn {
            itemsIndexed(filter.groupResults) { index, result ->
                val count = filter.groupResults[index].associationResults.size
                ListItem(
                    headlineContent = {
                        Text(text = result.name, modifier = Modifier.padding(start = 16.dp))
                    },
                    trailingContent = {
                        Text(
                            text = "$count",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    overlineContent = {
                    },
                    modifier = Modifier.clickable {
                        onClick(index)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    if (index < count - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Displays the provided list of associations [results] for the given [element] grouped by
 * [UtilityNetworkSource].
 */
@Composable
internal fun Associations(
    title : String,
    results: List<UAResult>,
    //source: UtilityElement,
    onUtilityElementClick: (ArcGISFeature) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberSaveable(inputs = arrayOf(), saver = LazyListState.Saver) {
        LazyListState()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        Column {
            Header(
                "Layer Name",
                title,
                onBackPressed = onBackPressed,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(shape = RoundedCornerShape(15.dp)),
                state = lazyListState
            ) {
                results.forEachIndexed { index, result ->
                    item(result.association.hashCode()) {
                        AssociationItem(
                            association = result.association,
                            arcGISFeature = result.associatedFeature,
                            onClick = {
                                onUtilityElementClick(result.associatedFeature)
                            }
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            if (index < results.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociationItem(
    association: UtilityAssociation,
    arcGISFeature: ArcGISFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = association.getTargetElement(arcGISFeature)
    ListItem(
        headlineContent = {
            Text(
                text = "Object ID ${arcGISFeature.objectId}",
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        supportingContent = {
            Text(
                text = target.assetGroup.name,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier.clickable {
            onClick()
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}
