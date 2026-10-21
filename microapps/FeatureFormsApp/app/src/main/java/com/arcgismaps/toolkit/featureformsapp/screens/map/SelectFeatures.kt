/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureformsapp.screens.login.verticalScrollbar

@Composable
fun SelectFeaturesDialog(
    viewModel: SelectFeaturesViewModel,
    onFeaturesSelected: (List<ArcGISFeature>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val features = viewModel.availableFeatures
    val lazyListState = rememberLazyListState()
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .widthIn(max = 400.dp)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Header(
                    featureCount = viewModel.featureCount,
                    selectedFeatureCountProvider = { viewModel.selectedFeatures.size },
                    onEdit = {
                        onFeaturesSelected(viewModel.selectedFeatures)
                    }
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Select the feature(s) you want to edit",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(thickness = 2.dp)
                Spacer(modifier = Modifier.height(5.dp))
                LazyColumn(
                    modifier = Modifier
                        .wrapContentHeight()
                        .verticalScrollbar(lazyListState),
                    state = lazyListState
                ) {
                    features.keys.forEachIndexed { index, layer ->
                        item {
                            Text(
                                text = layer,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                        items(features[layer]!!) { feature ->
                            FeatureItem(
                                feature = feature,
                                onChecked = { checked ->
                                    if (checked) {
                                        viewModel.addFeature(feature)
                                    } else {
                                        viewModel.removeFeature(feature)
                                    }
                                }
                            )
                        }
                        if (index < features.keys.size - 1) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    thickness = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    featureCount: Int,
    selectedFeatureCountProvider: () -> Int,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Features ($featureCount)",//stringResource(R.string.multiple_features, state.featureCount),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        FilledIconButton(
            onClick = onEdit,
            enabled = selectedFeatureCountProvider() > 0
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Features")
        }
    }
}


@Composable
private fun FeatureItem(
    feature: ArcGISFeature,
    modifier: Modifier = Modifier,
    onChecked: (Boolean) -> Unit,
) {
    val resources = LocalResources.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var checked by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = {
            Text(text = feature.label)
        },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = true
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = modifier.clickable {
            checked = !checked
            onChecked(checked)
        },
        trailingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                // set the color of the surface to white since the bitmap does not support
                // dark mode
                color = Color.White
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    )

    LaunchedEffect(feature) {
        bitmap = feature.getSymbol(resources)
    }
}
