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

package com.arcgismaps.toolkit.featureforms.internal.editor

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormBrowserState
import com.arcgismaps.toolkit.featureforms.internal.components.material3.ModalBottomSheet
import com.arcgismaps.toolkit.featureforms.internal.components.material3.ModalBottomSheetProperties
import com.arcgismaps.toolkit.featureforms.internal.components.material3.rememberModalBottomSheetState
import com.arcgismaps.toolkit.featureforms.internal.screens.verticalScrollbar
import kotlinx.coroutines.launch

@Composable
public fun FeatureFormNavigationBar(
    state: FeatureFormBrowserState,
    modifier: Modifier = Modifier,
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
) {
    val forms by state.featureForms.collectAsState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var expand by remember {
        mutableStateOf(false)
    }
    FeatureFormNavigationBar(
        activeForm = state.featureFormState.activeFeatureForm,
        featureForms = forms,
        onFeatureFormSelected = { featureForm ->
            state.featureFormState.navigateToForm(featureForm)
        },
        onExpand = {
            expand = true
        },
        modifier = modifier
    )
    if (expand) {
        ModalBottomSheet(
            onDismissRequest = {
                expand = false
            },
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "All Forms (${forms.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                expand = false
                            }
                        },
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .heightIn(max = 150.dp)
                        .verticalScrollbar(
                            state = lazyListState,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            width = 4.dp,
                            offsetX = 0.dp,
                            autoHide = false
                        )
                ) {
                    items(forms) { featureForm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable {
                                    state.featureFormState.navigateToForm(featureForm)
                                    expand = false
                                }.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Article,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = featureForm.feature.objectId,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            IconButton(onClick = {
                                onShowOnMapRequest(featureForm.feature)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
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
internal fun BrowserOverview(
    forms: List<FeatureForm>,
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "All Forms (${forms.size})",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(12.dp)
            )
            IconButton(
                onClick = {
//                    scope.launch {
//                        sheetState.hide()
//                    }.invokeOnCompletion {
//                        expand = false
//                    }
                },
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .heightIn(max = 150.dp)
                .verticalScrollbar(
                    state = lazyListState,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    width = 4.dp,
                    offsetX = 0.dp,
                    autoHide = false
                )
        ) {
            items(forms) { featureForm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable {
//                            state.featureFormState.navigateToForm(featureForm)
//                            expand = false
                        }.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = featureForm.feature.objectId,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = {
                        onShowOnMapRequest(featureForm.feature)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureFormNavigationBar(
    activeForm: FeatureForm,
    featureForms: List<FeatureForm>,
    onFeatureFormSelected: (FeatureForm) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = featureForms.indexOfFirst {
        it.feature == activeForm.feature
    }
    val previousForm = featureForms.getOrNull(activeIndex - 1)
    val nextForm = featureForms.getOrNull(activeIndex + 1)

    Log.e("TAG", "FeatureFormNavigationBar: $activeIndex/${featureForms.count()}")
    Log.e("TAG", "FeatureFormNavigationBar: prev ${previousForm?.feature?.objectId}")
    Log.e("TAG", "FeatureFormNavigationBar: next ${nextForm?.feature?.objectId}")

    FeatureFormNavigationBarContent(
        title = activeForm.feature.objectId,
        activeIndex = activeIndex,
        formCount = featureForms.size,
        onPrevious = { previousForm?.let(onFeatureFormSelected) },
        onNext = { nextForm?.let(onFeatureFormSelected) },
        onExpand = onExpand,
        modifier = modifier
    )
}

@Composable
private fun FeatureFormNavigationBarContent(
    title: String,
    activeIndex: Int,
    formCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(
            topStart = 25.dp,
            topEnd = 25.dp
        ),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics {
                    stateDescription = "${activeIndex + 1} of $formCount"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(activeIndex > 0) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = activeIndex > 0,
                        colors = IconButtonDefaults.iconButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.30f
                            )
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Previous form",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpand() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "$formCount Features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(activeIndex < (formCount - 1)) {
                    IconButton(
                        onClick = onNext,
                        enabled = activeIndex < (formCount - 1),
                        colors = IconButtonDefaults.iconButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.30f
                            )
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Next form",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the global ID of the [ArcGISFeature].
 */
internal val ArcGISFeature.objectId: String
    get() = (attributes["OBJECTID"] as? Long?).toString()

@Preview
@Composable
private fun FeatureFormNavigationBarPreview() {
    FeatureFormNavigationBarContent(
        title = "Feature Form Title",
        activeIndex = 1,
        formCount = 3,
        onPrevious = {},
        onNext = {},
        onExpand = {}
    )
}
