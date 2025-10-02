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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.layers.ArcGISSublayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.utils.SharedImageLoader
import kotlinx.coroutines.launch

/**
 * A screen that displays a list of features that can be associated with a utility network feature.
 *
 * @param viewModel The [AddAssociationFromSourceViewModel] that provides the data for the screen.
 * @param onBackPressed A callback that is called when the back button is pressed.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun SelectAssociatedFeatureScreen(
    viewModel: AddAssociationFromSourceViewModel,
    onBackPressed: () -> Unit,
    onFeatureCandidateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val title = viewModel.selectedAssetType?.name ?: ""
    val subTitle = viewModel.selectedSource?.name ?: ""
    val pagedData = viewModel.featureCandidateFlow.collectAsLazyPagingItems()
    val context = LocalContext.current
    val imageLoader = remember {
        SharedImageLoader.get(context)
    }
    Column(modifier = modifier) {
        AddWorkflowTopBar(
            title = title,
            subTitle = subTitle,
            onBackPressed = onBackPressed,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedContent(
            targetState = pagedData.loadState.refresh
        ) { loadState ->
            when (loadState) {
                is LoadState.Loading -> {
                    LoadingRow(modifier = Modifier.fillMaxWidth())
                }

                is LoadState.NotLoading -> {
                    Surface(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceBright
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.verticalScrollbar(
                                state = lazyListState,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                width = 4.dp,
                                offsetX = 4.dp,
                                autoHide = true
                            )
                        ) {
                            items(count = pagedData.itemCount) { index ->
                                val item = pagedData[index]
                                if (item != null) {
                                    ListItem(
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                viewModel.selectFeatureCandidate(item)
                                                onFeatureCandidateSelected()
                                            }
                                        },
                                        headlineContent = {
                                            Text(
                                                text = item.title,
                                                modifier = Modifier.padding(
                                                    start = 4.dp,
                                                    top = 4.dp,
                                                    bottom = 4.dp
                                                )
                                            )
                                        },
                                        leadingContent = {
                                            AsyncImage(
                                                model = item.feature.getSymbol(),
                                                contentDescription = "Feature Symbol",
                                                imageLoader = imageLoader,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    // TODO: Handle locate feature on map
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocationSearching,
                                                    contentDescription = "Locate Feature",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                                        )
                                    )
                                    if (index < pagedData.itemCount - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    }
                                }
                            }

                            if (pagedData.loadState.append is LoadState.Loading) {
                                item {
                                    LoadingRow(modifier = Modifier.fillMaxWidth())
                                }
                            }

                            if (pagedData.loadState.append is LoadState.Error) {
                                item {
                                    val loadState = (pagedData.loadState.append as LoadState.Error)
                                    ErrorRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        message = "${loadState.error.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                }

                is LoadState.Error -> {
                    ErrorRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        message = "${loadState.error.localizedMessage}"
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(56.dp)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ErrorRow(modifier: Modifier = Modifier, message: String) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Error loading features.",
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = message,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Returns the symbol of the feature. If the feature's layer is a subtype feature layer,
 * it will return the symbol of the sublayer that the feature belongs to. If the feature's layer is
 * a feature layer, it will return the symbol of the feature layer.
 *
 * If the symbol is not available, it will return null.
 */
internal fun ArcGISFeature.getSymbol(): Symbol? {
    val renderer = when (featureTable?.layer) {
        is SubtypeFeatureLayer -> sublayer?.renderer
        is FeatureLayer -> (featureTable?.layer as? FeatureLayer)?.renderer
        else -> null
    }
    return renderer?.getSymbol(this)
}

/**
 * Returns the sublayer that the feature belongs to. If the feature's layer is not a subtype feature
 * layer, it will return null.
 */
internal val ArcGISFeature.sublayer: ArcGISSublayer?
    get() {
        val subtypeFeatureLayer = featureTable?.layer as? SubtypeFeatureLayer ?: return null
        val code = getFeatureSubtype()?.code ?: return null
        return subtypeFeatureLayer.getSublayerWithSubtypeCode(code)
    }

/**
 * A modifier that adds a vertical scrollbar to a scrollable content.
 */
internal fun Modifier.verticalScrollbar(
    state: LazyListState,
    trackColor: Color,
    color: Color,
    width: Dp,
    offsetX: Dp,
    autoHide: Boolean = true
): Modifier = this
    .padding(end = offsetX)
    .then(
        this.composed {
            // fade in fast when scrolling, fade out slow when not scrolling
            val duration = if (state.isScrollInProgress) 50 else 500
            // animate the scrollbar alpha based on the scroll state
            val alpha by animateFloatAsState(
                targetValue = if (!autoHide || state.isScrollInProgress) 1f else 0f,
                animationSpec = tween(durationMillis = duration),
                label = ""
            )

            drawWithContent {
                drawContent()

                val firstVisibleElement =
                    state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@drawWithContent
                val itemHeight = firstVisibleElement.size.toFloat()
                val totalHeight = itemHeight * state.layoutInfo.totalItemsCount
                val scrollbarHeight = minOf(size.height / totalHeight, 1f) * size.height
                // Do not draw scrollbar if it is not needed
                if (scrollbarHeight >= size.height) return@drawWithContent
                // Calculate the Y offset of the scrollbar
                val scrollBarOffsetY = (size.height / totalHeight) *
                    (state.firstVisibleItemIndex * itemHeight + state.firstVisibleItemScrollOffset)
                // Calculate the X offset of the scrollbar
                val scrollBarOffsetX = size.width + width.toPx() - offsetX.toPx()

                // draw the scroll bar track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(scrollBarOffsetX, 0f),
                    size = Size(width.toPx(), size.height),
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = alpha
                )
                // draw the scroll bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(scrollBarOffsetX, scrollBarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = alpha
                )
            }
        }
    )
