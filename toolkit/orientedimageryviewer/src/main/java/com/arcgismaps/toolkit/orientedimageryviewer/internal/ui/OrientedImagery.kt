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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageType
import com.arcgismaps.toolkit.orientedimageryviewer.ImageNavigationStatus
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.arcgismaps.toolkit.orientedimageryviewer.internal.components.PanoramicImage
import com.arcgismaps.toolkit.orientedimageryviewer.internal.components.RasterImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Displays search results in a button-controlled pager, or the active adjacent image in sequential
 * mode. Paging gestures are disabled so each renderer retains its own pan, zoom and rotation gestures.
 *
 * @param viewerState The state of the oriented imagery viewer.
 * @param modifier The modifier to be applied to the imagery container.
 */
@Composable
internal fun OrientedImagery(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    onPopup: (() -> Unit)? = null,
    onError: (String) -> Unit = {},
) {
    val viewerUiState = viewerState.uiState

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 520.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            when {
                viewerUiState.activeImageInfo == null -> {
                    val localizedMessage = viewerUiState.messageResourceId?.let { stringResource(it) }
                    MessageScreen(
                        message = localizedMessage ?: viewerUiState.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> key(viewerState) {
                    val images = viewerState.searchResultImages
                    // Retain the selected page even when a mode change moves it to index zero.
                    // Numeric keys are saveable; SDK image objects are not.
                    val pageKeys = remember { ImagePageKeys() }
                    SideEffect { pageKeys.retainOnly(images) }
                    val pager = rememberPagerState(
                        initialPage = viewerUiState.selectedResultIndex.coerceAtLeast(0),
                        pageCount = { images.size }
                    )
                    LaunchedEffect(viewerUiState.selectedResultIndex) {
                        val index = viewerUiState.selectedResultIndex
                        if (index in images.indices) pager.scrollToPage(index)
                    }
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxSize().testTag("OrientedImagePager"),
                        userScrollEnabled = false,
                        key = { pageKeys.keyFor(images[it]) }
                    ) { index ->
                        val image = images[index]
                        val active = index == viewerUiState.selectedResultIndex
                        val loaded by produceState<Result<Unit>?>(null, image, active) {
                            value = try {
                                // The session has already loaded the selected image.
                                if (active) Result.success(Unit) else image.load()
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                Result.failure(error)
                            }
                            value?.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when {
                                loaded == null -> CircularProgressIndicator()
                                loaded?.isFailure == true -> MessageScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    message = stringResource(R.string.image_load_failed)
                                )
                                else -> ImageContent(viewerState, image, active, onPopup, onError)
                            }
                            if (!viewerUiState.isSequentialNavigationEnabled) Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        if (viewerUiState.activeImageInfo != null) ImageNavigationControls(viewerState, onError)
    }
}

private class ImagePageKeys {
    private val keys = mutableMapOf<OrientedImage, Long>()
    private var nextKey = 0L

    fun keyFor(image: OrientedImage): Long = keys.getOrPut(image) { nextKey++ }

    fun retainOnly(images: List<OrientedImage>) {
        keys.keys.retainAll(images.toSet())
    }
}

@Composable
private fun ImageContent(
    viewerState: OrientedImageryViewerState,
    image: OrientedImage? = null,
    isActive: Boolean = true,
    onPopup: (() -> Unit)? = null,
    onError: (String) -> Unit = {}
) {
    val dataUri = if (image != null) image.dataUri else viewerState.uiState.activeImageInfo?.dataUri
    val type = if (image != null) image.type else viewerState.uiState.activeImageInfo?.type
    when {
        dataUri == null -> MessageScreen(
            modifier = Modifier.fillMaxSize(), message = stringResource(R.string.image_source_missing)
        )
        type == OrientedImageType.Image360 -> PanoramicImage(
            viewerState, Modifier.fillMaxSize(), pageImage = image, isActive = isActive, onPopup = onPopup,
            onError = onError
        )
        type == OrientedImageType.Horizontal || type == OrientedImageType.Inspection ||
                type == OrientedImageType.Nadir || type == OrientedImageType.Oblique -> RasterImage(
            viewerState, Modifier.fillMaxSize(), pageImage = image, isActive = isActive, onPopup = onPopup,
            onError = onError
        )
        else -> MessageScreen(
            modifier = Modifier.fillMaxSize(), message = stringResource(R.string.image_type_unsupported)
        )
    }
}

@Composable
private fun ImageNavigationControls(viewerState: OrientedImageryViewerState, onError: (String) -> Unit) {
    val state = viewerState.uiState
    val sequential = state.isSequentialNavigationEnabled
    val scope = rememberCoroutineScope()
    val reportError by rememberUpdatedState(onError)
    val failureMessage = stringResource(R.string.image_navigation_failed)
    var prefetchFailureReported by remember(viewerState, state.activeImageRevision, state.imageSessionRevision, sequential) {
        mutableStateOf(false)
    }
    fun navigate(operation: suspend () -> Result<ImageNavigationStatus>) {
        scope.launch {
            val result = operation()
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            ensureActive()
            val current = viewerState.uiState
            if (result.isFailure && current.imageSessionRevision == state.imageSessionRevision &&
                current.isSequentialNavigationEnabled == sequential
            ) reportError(failureMessage)
        }
    }
    val canPrevious = if (sequential) state.canNavigatePrevious else state.selectedResultIndex > 0
    val canNext = if (sequential) state.canNavigateNext else state.selectedResultIndex < state.searchResultCount - 1
    val hasPrefetchError = sequential && (state.previousImageError != null || state.nextImageError != null)
    LaunchedEffect(viewerState, state.activeImageRevision, state.imageSessionRevision, sequential, hasPrefetchError) {
        val current = viewerState.uiState
        if (current.activeImageRevision != state.activeImageRevision ||
            current.imageSessionRevision != state.imageSessionRevision ||
            current.isSequentialNavigationEnabled != sequential
        ) return@LaunchedEffect
        if (hasPrefetchError && !prefetchFailureReported) {
            prefetchFailureReported = true
            reportError(failureMessage)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.Center) {
            if (!sequential) {
                IconButton(enabled = canPrevious, onClick = { navigate { viewerState.navigateFirstImage() } }) {
                    Icon(Icons.Filled.FirstPage, stringResource(R.string.toolbar_first_image))
                }
            }
            IconButton(enabled = canPrevious, onClick = { navigate { viewerState.navigatePreviousImage() } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.toolbar_previous_image))
            }
            IconButton(enabled = canNext, onClick = { navigate { viewerState.navigateNextImage() } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.toolbar_next_image))
            }
            if (!sequential) {
                IconButton(enabled = canNext, onClick = { navigate { viewerState.navigateLastImage() } }) {
                    Icon(Icons.AutoMirrored.Filled.LastPage, stringResource(R.string.toolbar_last_image))
                }
            }
        }
    }
}
