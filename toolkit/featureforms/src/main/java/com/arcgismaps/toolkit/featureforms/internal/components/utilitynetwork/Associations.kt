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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetworkSource

@Composable
internal fun UtilityNetworkAssociationLayers(
    group: UtilityAssociationGroup,
    source: UtilityElement,
    onBackPressed: () -> Unit,
    onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLayer by rememberSaveable { mutableStateOf<String?>(null) }
    val title = if (selectedLayer != null) {
        selectedLayer.toString()
    } else {
        group.type.name
    }
    val backHandler by rememberUpdatedState {
        if (selectedLayer != null) {
            selectedLayer = null
        } else {
            onBackPressed()
        }
    }
    Surface(modifier = modifier) {
        Column() {
            Header(
                title,
                source.objectId.toString(),
                onBackPressed = backHandler,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            HorizontalDivider()
            AnimatedContent(
                targetState = selectedLayer, label = "associations",
            ) { targetState ->
                if (targetState == null) {
                    Layers(
                        group = group,
                        onClick = {
                            selectedLayer = it
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Associations(
                        associations = group.elements[targetState] ?: emptyList(),
                        source = source,
                        onUtilityElementClick = onUtilityElementClick,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    BackHandler(onBack = backHandler)
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
    group: UtilityAssociationGroup,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val layers = group.getLayers()
    // show the list of layers
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn {
            itemsIndexed(layers) { index, layer ->
                val count = group.elements[layer]?.size ?: 0
                ListItem(
                    headlineContent = {
                        Text(text = layer, modifier = Modifier.padding(start = 16.dp))
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
                        onClick(layer)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    if (index < layers.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Displays the provided list of associations [associations] for the given [element] grouped by
 * [UtilityNetworkSource].
 */
@Composable
internal fun Associations(
    associations: List<UtilityAssociation>,
    source: UtilityElement,
    onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn {
            itemsIndexed(associations) { index, association ->
                val target = association.getTargetElement(source)
                AssociationItem(
                    target,
                    onUtilityElementClick
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    if (index < associations.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociationItem(
    element: UtilityElement,
    onClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Object ID ${element.objectId}",
                modifier = Modifier.padding(start = 16.dp)
            )
        },
        supportingContent = {
            Text(
                text = element.assetGroup.name,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier.clickable {
            onClick(element)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}
