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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.Guid
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityElement

/**
 * Displays the provided list of associations that are part of the
 * [com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult].
 *
 * @param groupResult The [UtilityAssociationGroupResult] to display.
 * @param isNavigationEnabled Whether navigation to the associated feature is enabled.
 * @param onItemClick A callback that is called when an association is clicked.
 * @param onDetailsClick A callback that is called when the details icon is clicked.
 * @param onDelete A callback that is called when an association is deleted. The callback provides
 * a boolean indicating whether the group is empty after deletion.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun UtilityAssociationGroupResult(
    groupResult: MutableGroupResult,
    isEditable: Boolean,
    isNavigationEnabled: Boolean,
    onAssociatedFeatureLocateRequest: (ArcGISFeature) -> Unit,
    onItemClick: (Int) -> Unit,
    onDetailsClick: (Int) -> Unit,
    onDelete: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    Surface(
        modifier = modifier.wrapContentHeight(
            align = Alignment.Top
        ),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        LazyColumn(
            modifier = Modifier.clip(shape = RoundedCornerShape(15.dp)),
            state = lazyListState
        ) {
            groupResult.associationResults.forEachIndexed { index, info ->
                item {
                    AssociationItem(
                        title = info.title,
                        association = info.association,
                        associatedFeature = info.associatedFeature,
                        isEditable = isEditable,
                        enabled = isNavigationEnabled,
                        onClick = {
                            onItemClick(index)
                        },
                        onDetailsClick = {
                            onDetailsClick(index)
                        },
                        onDelete = {
                            groupResult.delete(info.association)
                            onDelete(groupResult.associationResults.isEmpty())
                        },
                        onLocateRequest = {
                            onAssociatedFeatureLocateRequest(info.associatedFeature)
                        },
                        modifier = Modifier.animateItem()
                    )
                    if (index < groupResult.associationResults.count() - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
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
 * The spec for displaying the association is based on the following rules:
 *
 * - If the association is of type JunctionEdgeObjectConnectivityMidspan,
 * JunctionEdgeObjectConnectivityFromSide, JunctionEdgeObjectConnectivityToSide
 * then percentAlong and the terminal (if present) is displayed.
 *
 * - For a Connectivity association, the terminal (if present) is displayed.
 *
 * - For a Containment association, the isContainmentVisible property is displayed if the associated
 * feature is the toElement.
 *
 */
@Composable
private fun AssociationItem(
    title: String,
    association: UtilityAssociation,
    associatedFeature: ArcGISFeature,
    isEditable: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDelete: () -> Unit,
    onLocateRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var showMenu by rememberSaveable {
        mutableStateOf(false)
    }
    val density = LocalDensity.current
    // State to track a pending swipe state. This is used to set the desired swipe value in
    // a LaunchedEffect
    var pendingSwipeValue by remember {
        mutableStateOf<SwipeToDismissBoxValue?>(null)
    }
    val swipeToDismissBoxState = remember(association) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = {
                with(density) { 56.dp.toPx() }
            }
        )
    }
    val target = association.getTargetElement(associatedFeature)
    // Text to display below the title.
    var supportingText = ""
    when (association.associationType) {
        is UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan,
        UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide
            -> {
            supportingText = if (target.terminal != null) {
                "${target.terminal?.name}, ${(association.fractionAlongEdge * 100).toInt()}%"
            } else {
                "${(association.fractionAlongEdge * 100).toInt()}%"
            }
        }

        is UtilityAssociationType.Connectivity -> {
            target.terminal?.let { terminal ->
                supportingText = terminal.name
            }
        }

        is UtilityAssociationType.Containment -> {
            if (associatedFeature.globalId == association.toElement.globalId) {
                supportingText = stringResource(
                    R.string.containment_visible_value,
                    association.isContainmentVisible
                )
            }
        }

        else -> {}
    }
    val contentColor = if (enabled) {
        LocalContentColor.current
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = isEditable,
        backgroundContent = {
            // Cross-fade the background color as the drag gesture progresses.
            val color by animateColorAsState(
                when (swipeToDismissBoxState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart ->
                        lerp(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.error,
                            swipeToDismissBoxState.progress
                        )

                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "item swipe background color animation"
            )
            Row(
                modifier = Modifier
                    .background(color)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when (swipeToDismissBoxState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        Spacer(modifier = Modifier)
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove item",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .padding(12.dp)
                        )
                    }

                    else -> {}
                }
            }
        },
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .fillMaxWidth(),
        onDismiss = { value ->
            // Show the delete confirmation dialog when a swipe to delete is performed.
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
                // The actual swipe value will be set in a LaunchedEffect when the user confirms the
                // deletion.
                pendingSwipeValue = value
            }
        }
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            association.getIcon()?.let { icon ->
                Icon(
                    painter = icon,
                    contentDescription = "feature association icon",
                    modifier = Modifier.padding(
                        end = 12.dp
                    )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        top = if (supportingText.isNotEmpty()) 6.dp else 0.dp,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    color = contentColor
                )
                if (supportingText.isNotEmpty()) {
                    Text(
                        text = supportingText,
                        modifier = Modifier
                            .padding(
                                top = 6.dp,
                                bottom = 6.dp
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = contentColor
                    )
                }
            }
            Box {
                // Details button
                IconButton(onClick = {
                    showMenu = true
                }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "more information")
                }
                OptionsMenu(
                    editable = isEditable,
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    isOnLocateEnabled = associatedFeature.geometry != null,
                    onLocate = {
                        showMenu = false
                        onLocateRequest()
                    },
                    onMoreInfo = {
                        showMenu = false
                        onDetailsClick()
                    },
                    onRemove = {
                        showMenu = false
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
    if (showDeleteDialog) {
        // Confirmation dialog to delete the association
        RemoveAssociationConfirmationDialog(
            onDismiss = {
                Snapshot.withMutableSnapshot {
                    showDeleteDialog = false
                    pendingSwipeValue = SwipeToDismissBoxValue.Settled
                }
            },
            onRemove = {
                showDeleteDialog = false
                onDelete()
            },
        )
    }
    LaunchedEffect(pendingSwipeValue) {
        // Set the desired swipe value when there is a pending value
        pendingSwipeValue?.let {
            swipeToDismissBoxState.dismiss(it)
            pendingSwipeValue = null
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
 * Returns an icon for the association based on the association type.
 */
@Composable
internal fun UtilityAssociation.getIcon(): Painter? {
    return when (this.associationType) {

        is UtilityAssociationType.Connectivity -> {
            painterResource(R.drawable.connection_to_connection)
        }

        is UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide -> {
            painterResource(R.drawable.connection_end_left)
        }

        is UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan -> {
            painterResource(R.drawable.connection_mid)
        }

        is UtilityAssociationType.JunctionEdgeObjectConnectivityToSide -> {
            painterResource(R.drawable.connection_end_right)
        }

        else -> null
    }
}

@Composable
private fun OptionsMenu(
    editable: Boolean,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isOnLocateEnabled: Boolean,
    onLocate: () -> Unit,
    onMoreInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(15.dp)
    ) {
        if (isOnLocateEnabled) {
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.show_on_map))
                },
                onClick = onLocate,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LocationSearching,
                        contentDescription = "locate feature"
                    )
                }
            )
        }
        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.more_information))
            },
            onClick = onMoreInfo,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "association details"
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.remove))
            },
            onClick = onRemove,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "delete association"
                )
            },
            enabled = editable
        )
    }
}
