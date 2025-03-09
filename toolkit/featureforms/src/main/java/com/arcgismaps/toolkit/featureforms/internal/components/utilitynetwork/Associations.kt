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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.Guid
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationResult
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetworkSource
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType

@Composable
internal fun UtilityAssociationFilter(
    filterResult: UtilityAssociationsFilterResult,
    onGroupClick: (Int) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // show the list of layers
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn(modifier = Modifier) {
            filterResult.groupResults.forEachIndexed { index, group ->
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
                        overlineContent = {
                        },
                        modifier = Modifier.clickable {
                            onGroupClick(index)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    if (index < filterResult.groupResults.count() - 1) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
    BackHandler(onBack = onBackPressed)
}

/**
 * Displays the provided list of associations [results] for the given [element] grouped by
 * [UtilityNetworkSource].
 */
@Composable
internal fun Associations(
    groupResult: UtilityAssociationGroupResult,
    onItemClick: (UtilityAssociationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberSaveable(
        inputs = arrayOf(),
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .clip(shape = RoundedCornerShape(15.dp)),
            state = lazyListState
        ) {
            groupResult.associationResults.forEachIndexed { index, info ->
                item(info.association.hashCode()) {
                    AssociationItem(
                        association = info.association,
                        associatedFeature = info.associatedFeature,
                        onClick = {
                            onItemClick(info)
                        }
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        if (index < groupResult.associationResults.count() - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
    associatedFeature: ArcGISFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = association.getTargetElement(associatedFeature)
    val icon = association.getIcon(associatedFeature.globalId)
    val (terminal, fractionAlongEdge) = when (association.associationType) {
        is UtilityAssociationType.Connectivity -> {
            Pair(target.terminal, null)
        }

        is UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan -> {
            if (target.networkSource.sourceType == UtilityNetworkSourceType.Edge) {
                Pair(null, association.fractionAlongEdge)
            } else {
                Pair(target.terminal, null)
            }
        }

        else -> {
            Pair(null, null)
        }
    }
    val trailingText = when {
        terminal != null -> {
            "Terminal : ${terminal.name}"
        }

        fractionAlongEdge != null -> {
            "$fractionAlongEdge %"
        }

        association.associationType is UtilityAssociationType.Containment &&
            associatedFeature.globalId == association.toElement.globalId -> {
            "Containment Visible : ${association.isContainmentVisible}"
        }

        else -> ""
    }
    ListItem(
        headlineContent = {
            Text(
                text = associatedFeature.label,
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
        leadingContent = icon?.let {
            {
                Icon(
                    painter = it,
                    contentDescription = null
                )
            }
        },
        trailingContent = if (trailingText.isNotEmpty()) {
            {
                Text(
                    text = trailingText,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else null,
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}

internal fun UtilityAssociation.getTargetElement(arcGISFeature: ArcGISFeature): UtilityElement {
    return if (arcGISFeature.globalId == this.fromElement.globalId) {
        this.toElement
    } else {
        this.fromElement
    }
}

internal val ArcGISFeature.label: String
    get() {
        return if (objectId != null) {
            "Object ID : $objectId"
        } else if (attributes["name"] != null) {
            attributes["name"] as String
        } else {
            "Unnamed Feature"
        }
    }

internal val ArcGISFeature.objectId: Long?
    get() = attributes["objectid"] as? Long

internal val ArcGISFeature.globalId: Guid
    get() = attributes["globalid"] as Guid

@Composable
internal fun UtilityAssociation.getIcon(targetElementGuid: Guid): Painter? {
    return when (this.associationType) {
        is UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan -> {
            painterResource(R.drawable.connection_mid)
        }

        is UtilityAssociationType.Connectivity, UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide -> {
            if (targetElementGuid == fromElement.globalId) {
                painterResource(R.drawable.connection_end_left)
            } else {
                painterResource(R.drawable.connection_end_right)
            }
        }

        else -> null
    }
}
