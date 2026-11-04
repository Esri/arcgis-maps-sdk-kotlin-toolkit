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

package com.arcgismaps.toolkit.orientedimageryviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.ErrorSnackbarContainer
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.ExploreButton
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerToolsButton
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.MarkerOptionId
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.ModeSelectionButton
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.OrientedImagery
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.OverlaySelectionButton
import com.arcgismaps.toolkit.popup.Popup
import com.arcgismaps.toolkit.popup.PopupState
import kotlinx.coroutines.launch
import com.arcgismaps.mapping.popup.Popup as ImagePopup

/**
 * A composable that displays an oriented imagery viewer with a toolbar and content area.
 *
 * @param orientedImageryViewerState The state of the oriented imagery viewer.
 * @param modifier The modifier to be applied to the viewer layout.
 * @param onDismiss A callback that is invoked when the viewer is dismissed.
 * @param showCloseIcon Whether to show the close icon in the toolbar.
 */
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
        val onPopup: () -> Unit = {
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
        }
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
                            interactionStatus = viewerState.geoViewInteractionStatus,
                            isFindImagesSelected = viewerState.uiState.isFindImagesSelected,
                            isDisplayImageSelected = viewerState.uiState.isDisplayImageSelected,
                            onFindImagesSelectedChange = {
                                viewerState.setImageModeSelected(GeoViewInteractionStatus.FindImages, it)
                            },
                            onDisplayImageSelectedChange = {
                                viewerState.setImageModeSelected(GeoViewInteractionStatus.DisplayImage, it)
                            },
                            isMarkingLocation = viewerState.uiState.isMarkingLocation,
                            isMarkerSelectionVisible = viewerState.isMarkerSelectionVisible,
                            selectedMarkerOptionId = viewerState.selectedMarkerOptionId,
                            onSelectMarkerOption = viewerState::selectMarkerOption,
                            onDismissMarkerSelection = viewerState::dismissMarkerSelection,
                            hasActiveImage = viewerState.hasActiveImage,
                            hasAdditionalImages = viewerState.uiState.hasAdditionalImages,
                            isActiveFootprintVisible = viewerState.uiState.isActiveFootprintVisible,
                            isAdditionalFootprintsVisible = viewerState.uiState.isAdditionalFootprintsVisible,
                            isAdditionalCameraLocationsVisible = viewerState.uiState.isAdditionalCameraLocationsVisible,
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
                            onReset = onReset
                        )
                    }
                }
            },
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    OrientedImagery(
                        viewerState = viewerState,
                        modifier = Modifier.fillMaxSize(),
                        onPopup = onPopup.takeIf { imagePopup == null },
                        onError = errors::showError
                    )
                    imagePopup?.let { popup ->
                        // Scope evaluation to the popup's composition, including dismissal and image changes.
                        val popupScope = rememberCoroutineScope()
                        val popupState = remember(popup) { PopupState(popup, popupScope) }
                        // Cover the renderer with an opaque surface, including the popup header.
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.image_details_title),
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { imagePopup = null }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.image_details_close),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                HorizontalDivider()
                                Popup(
                                    popupState = popupState,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    onDismiss = { imagePopup = null },
                                    showCloseIcon = false
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
    interactionStatus: GeoViewInteractionStatus,
    isFindImagesSelected: Boolean,
    isDisplayImageSelected: Boolean,
    onFindImagesSelectedChange: (Boolean) -> Unit,
    onDisplayImageSelectedChange: (Boolean) -> Unit,
    isMarkingLocation: Boolean,
    isMarkerSelectionVisible: Boolean,
    selectedMarkerOptionId: MarkerOptionId?,
    onSelectMarkerOption: (MarkerOptionId) -> Unit,
    onDismissMarkerSelection: () -> Unit,
    hasActiveImage: Boolean,
    hasAdditionalImages: Boolean,
    isActiveFootprintVisible: Boolean,
    isAdditionalFootprintsVisible: Boolean,
    isAdditionalCameraLocationsVisible: Boolean,
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
    onReset: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ModeSelectionButton(
            interactionStatus = interactionStatus,
            isFindImagesSelected = isFindImagesSelected,
            isDisplayImageSelected = isDisplayImageSelected,
            onFindImagesSelectedChange = onFindImagesSelectedChange,
            onDisplayImageSelectedChange = onDisplayImageSelectedChange
        )
        OverlaySelectionButton(
            hasActiveImage = hasActiveImage,
            hasAdditionalImages = hasAdditionalImages,
            isActiveFootprintVisible = isActiveFootprintVisible,
            isAdditionalFootprintsVisible = isAdditionalFootprintsVisible,
            isAdditionalCameraLocationsVisible = isAdditionalCameraLocationsVisible,
            onCurrentFootprint = onCurrentFootprint,
            onAdditionalFootprints = onAdditionalFootprints,
            onAdditionalCameraLocations = onAdditionalCameraLocations
        )
        MarkerToolsButton(
            isMarkingLocation = isMarkingLocation,
            onAddMarker = onAddMarker,
            onSelectMarker = onSelectMarker,
            onClearAllMarkers = onClearAllMarkers,
            isMarkerSelectionVisible = isMarkerSelectionVisible,
            selectedOptionId = selectedMarkerOptionId,
            onSelectOption = onSelectMarkerOption,
            onDismissSelection = onDismissMarkerSelection
        )
        ExploreButton(
            supportsSequentialNavigation = supportsSequentialNavigation,
            hasActiveImage = hasActiveImage,
            isSequentialNavigationEnabled = isSequentialNavigationEnabled,
            onSequentialNavigation = onSequentialNavigation
        )
    }
}

@Preview
@Composable
internal fun ViewerLayoutPreview() {
    val coroutineScope = rememberCoroutineScope()

    ViewerLayout(
        viewerState = OrientedImageryViewerState(
            geoViewProxy = MapViewProxy(),
            orientedImageryLayer = OrientedImageryLayer("https://example.com/oriented-imagery-layer"),
            GraphicsOverlay(),
            coroutineScope = coroutineScope
        ),
        title = "Image 1 of 3",
        onPreviousImage = {},
        onNextImage = {}
    )
}
