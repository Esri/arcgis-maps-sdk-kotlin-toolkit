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

package com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.arcgismaps.toolkit.popup.R
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityElement

/**
 * Displays the provided list of associations that are part of the
 * [com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult].
 *
 * @param groupResult The [UtilityAssociationGroupResult] to display.
 * @param onItemClick A callback that is called when an association is clicked.
 * @param onDetailsClick A callback that is called when the details icon is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 * @param displayCount The number of associations to display.
 */
@Composable
internal fun UtilityAssociationGroupResult(
    groupResult: UtilityAssociationGroupResult,
    onItemClick: (Int) -> Unit,
    onDetailsClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    displayCount: Int = 3
) {
    val associationResults = groupResult.associationResults
    var showAll by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredResults = associationResults.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }
    val itemsToShow = if (showAll) filteredResults else filteredResults.take(displayCount)
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue ->
                searchQuery = newValue
                showAll = true
            },
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.filter_by_feature_title)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            modifier = Modifier.wrapContentHeight(align = Alignment.Top),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                LazyColumn(
                    modifier = Modifier.clip(shape = RoundedCornerShape(15.dp)),
                    state = lazyListState
                ) {
                    itemsToShow.forEachIndexed { index, info ->
                        item(info.association.hashCode()) {
                            AssociationItem(
                                title = info.title,
                                association = info.association,
                                associatedFeature = info.associatedFeature,
                                onClick = { onItemClick(index) },
                                onDetailsClick = { onDetailsClick(index) },
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                            if (index < itemsToShow.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = DividerDefaults.color.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    if (!showAll && filteredResults.size > displayCount) {
                        item("show_all") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAll = true }
                                    .padding(vertical = 12.dp, horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.show_all),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Total: ${filteredResults.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "List bullet",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
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
 * The spec for displaying the association is based on the following rules:
 *
 * - If the association is of type JunctionEdgeObjectConnectivityMidspan, then only the fractionAlongEdge
 * is displayed.
 *
 * - If the association is of type JunctionEdgeObjectConnectivityFromSide, JunctionEdgeObjectConnectivityToSide
 * or Connectivity then fractionAlongEdge and the terminal (if present) is displayed.
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
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val target = association.getTargetElement(associatedFeature)
    // Text to display at the end of the row.
    var trailingText = ""
    // Text to display below the title.
    var supportingText = ""
    when (association.associationType) {
        is UtilityAssociationType.JunctionEdgeObjectConnectivityMidspan -> {
            supportingText = if (target.terminal != null) {
                "${target.terminal?.name}, ${(association.fractionAlongEdge * 100).toInt()}%"
            } else {
                "${(association.fractionAlongEdge * 100).toInt()}%"
            }
        }

        is UtilityAssociationType.Connectivity,
        UtilityAssociationType.JunctionEdgeObjectConnectivityFromSide,
        UtilityAssociationType.JunctionEdgeObjectConnectivityToSide -> {
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
    val contentColor = LocalContentColor.current
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(56.dp)
            .fillMaxWidth(),
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
                maxLines = 1,
                color = contentColor
            )
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    modifier = Modifier
                        .padding(
                            bottom = 6.dp
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = contentColor
                )
            }
        }
        if (trailingText.isNotEmpty()) {
            Card(modifier = Modifier.wrapContentSize()) {
                Text(text = trailingText, modifier = Modifier.padding(8.dp))
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
