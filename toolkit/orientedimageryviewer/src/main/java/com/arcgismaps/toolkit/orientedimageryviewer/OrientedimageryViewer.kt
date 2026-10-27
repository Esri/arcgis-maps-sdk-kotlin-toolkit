/*
 *
 *  Copyright 2026 Esri
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
 *
 */

package com.arcgismaps.toolkit.orientedimageryviewer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.ErrorSnackbarContainer
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerToolsButton
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerSelection
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.OrientedImagery
import com.arcgismaps.toolkit.popup.Popup
import com.arcgismaps.toolkit.popup.PopupState
import kotlinx.coroutines.launch
import com.arcgismaps.mapping.popup.Popup as ImagePopup

@Composable
public fun OrientedImageryViewer(
    orientedImageryViewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
) {
    val coroutineScope = rememberCoroutineScope()
    val viewerUiState = orientedImageryViewerState.uiState

    ViewerLayout(
        viewerState = orientedImageryViewerState,
        modifier = modifier,
        title = orientedImageryViewerState.title,
        onPreviousImage = {
            coroutineScope.launch {
                orientedImageryViewerState.navigatePreviousImage()
            }
        },
        onNextImage = {
            coroutineScope.launch {
                orientedImageryViewerState.navigateNextImage()
            }
        },
        onCurrentFootprint = { orientedImageryViewerState.toggleActiveFootprint() },
        onAdditionalFootprints = { orientedImageryViewerState.toggleAdditionalFootprints() },
        onAdditionalCameraLocations = { orientedImageryViewerState.toggleAdditionalCameraLocations() },
        onAddMarker = { orientedImageryViewerState.addMarker() },
        onSelectMarker = { orientedImageryViewerState.selectMarker() },
        onClearAllMarkers = { orientedImageryViewerState.clearAllMarkers() },
        onSequentialNavigation = { orientedImageryViewerState.toggleSequentialNavigation() },
        onReset = { orientedImageryViewerState.resetAll()},
        onDismiss = onDismiss,
        showCloseIcon = showCloseIcon,
        isSequentialNavigationEnabled = viewerUiState.isSequentialNavigationEnabled
    )
}

@Composable
internal fun ViewerLayout(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    title: String = "Oriented imagery",
    onPreviousImage: (() -> Unit)? = null,
    onNextImage: (() -> Unit)? = null,
    onCurrentFootprint: () -> Unit = {},
    onAdditionalFootprints: () -> Unit = {},
    onAdditionalCameraLocations: () -> Unit = {},
    onAddMarker: () -> Unit = {},
    onSelectMarker: () -> Unit = {},
    onClearAllMarkers: () -> Unit = {},
    onSequentialNavigation: () -> Unit = {},
    onReset: () -> Unit = {},
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
    isSequentialNavigationEnabled: Boolean = false,
    onCreateImagePopup: suspend () -> Result<ImagePopup> = { viewerState.createImagePopup() },
) {
    val coroutineScope = rememberCoroutineScope()
    val imageRevision = viewerState.uiState.activeImageRevision
    var isCreatingPopup by remember(viewerState, imageRevision) { mutableStateOf(false) }
    // Key on transitions, not image metadata: a new search may return the same image.
    var imagePopup by remember(viewerState, viewerState.uiState.activeImageRevision) {
        mutableStateOf<ImagePopup?>(null)
    }
    val noActiveImageMessage = stringResource(R.string.popup_no_active_image)
    val creationFailedMessage = stringResource(R.string.popup_creation_failed)

    ErrorSnackbarContainer(modifier = modifier) { errors ->
        ViewerLayout(
            showTopBar = imagePopup == null,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showCloseIcon) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.toolbar_close)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrientedImageryToolbar(
                            isMarkingLocation = viewerState.uiState.isMarkingLocation,
                            onPreviousImage = onPreviousImage,
                            onNextImage = onNextImage,
                            onCurrentFootprint = onCurrentFootprint,
                            onAdditionalFootprints = onAdditionalFootprints,
                            onAdditionalCameraLocations = onAdditionalCameraLocations,
                            onAddMarker = onAddMarker,
                            onSelectMarker = onSelectMarker,
                            onClearAllMarkers = onClearAllMarkers,
                            supportsSequentialNavigation = viewerState.supportsSequentialNavigation,
                            isSequentialNavigationEnabled = isSequentialNavigationEnabled,
                            onSequentialNavigation = onSequentialNavigation,
                            onPopup = {
                                if (!isCreatingPopup) {
                                    isCreatingPopup = true
                                    coroutineScope.launch {
                                        try {
                                            val result = onCreateImagePopup()
                                            if (viewerState.uiState.activeImageRevision == imageRevision) {
                                                result.onSuccess { popup ->
                                                    viewerState.dismissMarkerSelection()
                                                    imagePopup = popup
                                                }.onFailure {
                                                    errors.showError(
                                                        if (viewerState.hasActiveImage) creationFailedMessage
                                                        else noActiveImageMessage
                                                    )
                                                }
                                            }
                                        } finally {
                                            isCreatingPopup = false
                                        }
                                    }
                                }
                            },
                            onReset = onReset
                        )
                    }
                }
            },
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    OrientedImagery(viewerState = viewerState, modifier = Modifier.fillMaxSize())
                    if (viewerState.isMarkerSelectionVisible && imagePopup == null) {
                        MarkerSelection(
                            selectedOptionId = viewerState.selectedMarkerOptionId,
                            onSelectOption = viewerState::selectMarkerOption,
                            onDismiss = viewerState::dismissMarkerSelection,
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        )
                    }
                    imagePopup?.let { popup ->
                        // Scope evaluation to the popup's composition, including dismissal and image changes.
                        val popupScope = rememberCoroutineScope()
                        val popupState = remember(popup) { PopupState(popup, popupScope) }
                        Popup(
                            popupState = popupState,
                            modifier = Modifier.fillMaxSize(),
                            onDismiss = { imagePopup = null },
                            showCloseIcon = false
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 2.dp,
                        ) {
                            IconButton(
                                onClick = { imagePopup = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.toolbar_close),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
internal fun ViewerLayout(
    topBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    Column(modifier = modifier) {
        if (showTopBar) {
            topBar()
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        }
        content()
    }
}

@Composable
private fun OrientedImageryToolbar(
    isMarkingLocation: Boolean,
    onPreviousImage: (() -> Unit)?,
    onNextImage: (() -> Unit)?,
    onCurrentFootprint: () -> Unit,
    onAdditionalFootprints: () -> Unit,
    onAdditionalCameraLocations: () -> Unit,
    onAddMarker: () -> Unit = {},
    onSelectMarker: () -> Unit = {},
    onClearAllMarkers: () -> Unit = {},
    supportsSequentialNavigation: Boolean,
    isSequentialNavigationEnabled: Boolean,
    onSequentialNavigation: () -> Unit,
    onPopup: () -> Unit,
    onReset: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = onPreviousImage != null,
            onClick = { onPreviousImage?.invoke() }
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_bold_left_24),
                contentDescription = stringResource(R.string.toolbar_previous_image),
                tint = Color.Unspecified
            )
        }
        IconButton(
            enabled = onNextImage != null,
            onClick = { onNextImage?.invoke() }
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_bold_right_24),
                contentDescription = stringResource(R.string.toolbar_next_image),
                tint = Color.Unspecified
            )
        }
        IconButton(onClick = onCurrentFootprint) {
            Icon(
                painter = painterResource(R.drawable.footprint_24),
                contentDescription = stringResource(R.string.toolbar_current_footprint),
                tint = Color.Red
            )
        }
        IconButton(onClick = onAdditionalFootprints) {
            Icon(
                painter = painterResource(R.drawable.footprint_24),
                contentDescription = stringResource(R.string.toolbar_additional_footprints),
                tint = Color.Blue
            )
        }
//        IconButton(onClick = onAdditionalCameraLocations) {
//            Icon(
//                painter = painterResource(R.drawable.additional_cameras_24),
//                contentDescription = stringResource(R.string.toolbar_additional_camera_locations),
//                tint = Color.Unspecified
//            )
//        }
        IconToggleButton(
            checked = isSequentialNavigationEnabled,
            enabled = supportsSequentialNavigation,
            onCheckedChange = { onSequentialNavigation() }
        ) {
            Icon(
                painter = painterResource(R.drawable.sequential_navigation_24),
                contentDescription = stringResource(R.string.toolbar_sequential_navigation)
            )
        }
        IconButton(onClick = onPopup) {
            Icon(
                painter = painterResource(R.drawable.popup_24),
                contentDescription = stringResource(R.string.toolbar_popup)
            )
        }
        MarkerToolsButton(
            isMarkingLocation = isMarkingLocation,
            onAddMarker = onAddMarker,
            onSelectMarker = onSelectMarker,
            onClearAllMarkers =onClearAllMarkers
        )
//        IconButton(onClick = onReset) {
//            Icon(
//                painter = painterResource(R.drawable.reset_24),
//                contentDescription = stringResource(R.string.toolbar_reset),
//                tint = Color.Unspecified
//            )
//        }
    }
}


@Preview
@Composable
internal fun ViewerLayoutPreview() {
    val coroutineScope = rememberCoroutineScope()

    ViewerLayout(
        viewerState = OrientedImageryViewerState(
            orientedImageryLayer = OrientedImageryLayer("https://example.com/oriented-imagery-layer"),
            GraphicsOverlay(),
            coroutineScope = coroutineScope
        ),
        title = "Image 1 of 3",
        onPreviousImage = {},
        onNextImage = {}
    )
}
