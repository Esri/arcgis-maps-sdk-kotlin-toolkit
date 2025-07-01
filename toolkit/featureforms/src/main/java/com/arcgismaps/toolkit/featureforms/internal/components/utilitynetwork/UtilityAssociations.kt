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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.Guid
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetworkSourceType

/**
 * Displays the provided [UtilityAssociationsFilterResult]. The filter result is displayed as a
 * list of its groups as given by [UtilityAssociationsFilterResult.groupResults].
 *
 * @param groupResults The [UtilityAssociationsFilterResult] to display.
 * @param onGroupClick A callback that is called when a group is clicked with the index of the group.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationFilter(
    groupResults: List<UtilityAssociationGroupResult>,
    onGroupClick: (UtilityAssociationGroupResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // show the list of layers
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
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
                        modifier = Modifier.clickable {
                            onGroupClick(group)
                        },
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

/**
 * Displays the provided list of associations that are part of the [UtilityAssociationGroupResult].
 *
 * @param groupResult The [UtilityAssociationGroupResult] to display.
 * @param onItemClick A callback that is called when an association is clicked.
 * @param onDetailsClick A callback that is called when the details icon is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociations(
    groupResult: UtilityAssociationGroupResult,
    onItemClick: (Int) -> Unit,
    onDetailsClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    Surface(
        modifier = modifier.wrapContentHeight(
            align = Alignment.Top
        ),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier.clip(shape = RoundedCornerShape(15.dp)),
            state = lazyListState
        ) {
            groupResult.associationResults.forEachIndexed { index, info ->
                item(info.association.hashCode()) {
                    AssociationItem(
                        title = info.title,
                        association = info.association,
                        associatedFeature = info.associatedFeature,
                        onClick = {
                            onItemClick(index)
                        },
                        onDetailsClick = {
                            onDetailsClick(index)
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    if (index < groupResult.associationResults.count() - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = DividerDefaults.color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the provided [UtilityAssociation] and its associated feature.
 *
 * If the association is of type JunctionEdgeObjectConnectivityFromSide, JunctionEdgeObjectConnectivityToSide
 * or JunctionEdgeObjectConnectivityMidspan and the target feature is an edge, the fractionAlongEdge
 * is displayed. Otherwise, the terminal is displayed.
 *
 * For a Connectivity association, the terminal is displayed.
 *
 * For a Containment association, the isContainmentVisible property is displayed if the associated
 * feature is the toElement.
 *
 */
@Composable
private fun AssociationItem(
    title: String,
    association: UtilityAssociation,
    associatedFeature: ArcGISFeature,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
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
            stringResource(R.string.terminal_with_value, terminal.name)
        }

        fractionAlongEdge != null -> {
            "$fractionAlongEdge %"
        }

        association.associationType is UtilityAssociationType.Containment &&
            associatedFeature.globalId == association.toElement.globalId -> {
            stringResource(R.string.containment_visible_value, association.isContainmentVisible)
        }

        else -> ""
    }
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = "feature association icon",
                    modifier = Modifier.padding(
                        end = 12.dp
                    )
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        top = 6.dp
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                if (trailingText.isNotEmpty()) {
                    Text(
                        text = trailingText,
                        modifier = Modifier
                            .padding(
                                bottom = 6.dp
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Returns the target [UtilityElement] of the association that equal to the provided [ArcGISFeature].
 */
internal fun UtilityAssociation.getTargetElement(arcGISFeature: ArcGISFeature): UtilityElement {
    return if (arcGISFeature.globalId == this.fromElement.globalId) {
        this.fromElement
    } else {
        this.toElement
    }
}

/**
 * Returns the global ID of the [ArcGISFeature].
 */
internal val ArcGISFeature.globalId: Guid
    get() = attributes["globalid"] as Guid

/**
 * Returns an icon for the association based on the association type and the GUID of the target element.
 */
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
