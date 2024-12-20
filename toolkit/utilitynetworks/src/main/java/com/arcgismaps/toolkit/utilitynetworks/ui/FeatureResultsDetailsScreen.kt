/*
 *  Copyright 2024 Esri
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
 */
package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ZoomInMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ExpandableCardWithLabel
import com.arcgismaps.toolkit.utilitynetworks.internal.util.UpButton
import com.arcgismaps.utilitynetworks.UtilityElement

/**
 * Composable that displays the selected groups of feature results.
 *
 * @since 200.6.0
 */
@Composable
internal fun FeatureResultsDetailsScreen(
    selectedGroupName: String,
    elementListWithSelectedGroupName: List<UtilityElement>,
    onFeatureSelected: (UtilityElement) -> Unit,
    onBackToResults: () -> Unit
) {
    BackHandler {
        onBackToResults()
    }

    Column {
        UpButton(stringResource(id = R.string.feature_results), onBackToResults)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = selectedGroupName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyColumn {
            item {
                FeatureList(elementListWithSelectedGroupName, onFeatureSelected)
            }
        }
    }
}

@Composable
private fun FeatureList(assetTypeList: List<UtilityElement>, onFeatureSelected: (UtilityElement) -> Unit) {
    ExpandableCardWithLabel(assetTypeList[0].assetType.name, contentTitle = assetTypeList.size.toString()) {
        Column {
            assetTypeList.forEach { utilityElement ->
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        .clickable { onFeatureSelected(utilityElement) },
                ) {
                    Icon(
                        imageVector = Icons.Sharp.ZoomInMap,
                        contentDescription = stringResource(R.string.zoom_in),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = stringResource(R.string.object_id, utilityElement.objectId),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
