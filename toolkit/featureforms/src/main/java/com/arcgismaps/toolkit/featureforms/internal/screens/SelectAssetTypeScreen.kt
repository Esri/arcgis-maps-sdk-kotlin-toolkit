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

package com.arcgismaps.toolkit.featureforms.internal.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.utils.SearchBar

@Composable
internal fun SelectAssetTypeScreen(
    viewModel: AddAssociationFromSourceViewModel,
    onBackPressed: () -> Unit,
    onAssetTypeSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkSource = viewModel.selectedSource
    val assetTypes = networkSource?.assetTypes ?: emptyList()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredAssetTypes = assetTypes.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = modifier) {
        AddWorkflowTopBar(
            title = "${networkSource?.name}",
            subTitle = "",
            onBackPressed = onBackPressed,
            modifier = Modifier.fillMaxWidth(),
        )
        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = stringResource(R.string.search)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.available_asset_types),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = stringResource(R.string.count, filteredAssetTypes.count()),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surfaceBright
        ) {
            LazyColumn {
                itemsIndexed(filteredAssetTypes) { index, assetType ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.selectAssetType(assetType)
                            onAssetTypeSelected()
                        },
                        headlineContent = {
                            Text(
                                text = assetType.name,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        )
                    )
                    if (index < filteredAssetTypes.count() - 1) {
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
