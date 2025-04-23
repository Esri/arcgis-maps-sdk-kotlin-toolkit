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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    if (index < groupResults.count() - 1) {
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
}

/**
 * Displays the provided list of associations that are part of the [UtilityAssociationGroupResult].
 *
 * @param groupResult The [UtilityAssociationGroupResult] to display.
 * @param onItemClick A callback that is called when an association is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociations(
    groupResult: UtilityAssociationGroupResult,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp)
    ) {
        LazyColumn(
            modifier = Modifier.clip(shape = RoundedCornerShape(15.dp)),
            state = lazyListState
        ) {
            groupResult.associationResults.forEachIndexed { index, info ->
                item(info.association.hashCode()) {
                    AssociationItem(
                        association = info.association,
                        associatedFeature = info.associatedFeature,
                        onClick = {
                            onItemClick(index)
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
            stringResource(R.string.terminal, terminal.name)
        }

        fractionAlongEdge != null -> {
            "$fractionAlongEdge %"
        }

        association.associationType is UtilityAssociationType.Containment &&
            associatedFeature.globalId == association.toElement.globalId -> {
            stringResource(R.string.containment_visible, association.isContainmentVisible)
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
 * Returns the label for the [ArcGISFeature]. This can be the object ID, the name attribute or a default
 * label if none of these are available.
 */
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

/**
 * Returns the object ID of the [ArcGISFeature] if available.
 */
internal val ArcGISFeature.objectId: Long?
    get() = attributes["objectid"] as? Long

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
