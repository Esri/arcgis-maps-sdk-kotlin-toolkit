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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.view.GraphicsOverlay

@Composable
public fun OrientedImageryViewer(
    orientedImageryViewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
) {
    ViewerLayout(
        viewerState = orientedImageryViewerState,
        modifier = modifier,
        title = orientedImageryViewerState.title,
        onPreviousImage = { orientedImageryViewerState.showPreviousImage() },
        onNextImage = { orientedImageryViewerState.showNextImage() },
        onCurrentFootprint = { orientedImageryViewerState.toggleActiveFootprint() },
        onAdditionalFootprints = { orientedImageryViewerState.toggleAdditionalFootprints() },
        onAdditionalCameraLocations = { orientedImageryViewerState.toggleAdditionalCameraLocations() },
        onSequentialNavigation = { orientedImageryViewerState.toggleSequentialNavigation() },
        onReset = { orientedImageryViewerState.resetAll()},
        onDismiss = onDismiss,
        showCloseIcon = showCloseIcon
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
    onSequentialNavigation: () -> Unit = {},
    onReset: () -> Unit = {},
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
) {
    ViewerLayout(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 8.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End,
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
                            contentDescription = "Close"
                        )
                    }
                }
                OrientedImageryToolbar(
                    onPreviousImage = onPreviousImage,
                    onNextImage = onNextImage,
                    onCurrentFootprint = onCurrentFootprint,
                    onAdditionalFootprints = onAdditionalFootprints,
                    onAdditionalCameraLocations = onAdditionalCameraLocations,
                    supportsSequentialNavigation = viewerState.supportsSequentialNavigation,
                    isSequentialNavigationEnabled = viewerState.isSequentialNavigationEnabled,
                    onSequentialNavigation = onSequentialNavigation,
                    onReset = onReset
                )
            }
        },
        content = {
            ImageryView(
                viewerState = viewerState,
            )
        }
    )
}

@Composable
internal fun ViewerLayout(
    topBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        topBar()
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        content()
    }
}

@Composable
private fun OrientedImageryToolbar(
    onPreviousImage: (() -> Unit)?,
    onNextImage: (() -> Unit)?,
    onCurrentFootprint: () -> Unit,
    onAdditionalFootprints: () -> Unit,
    onAdditionalCameraLocations: () -> Unit,
    supportsSequentialNavigation: Boolean,
    isSequentialNavigationEnabled: Boolean,
    onSequentialNavigation: () -> Unit,
    onReset: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = onPreviousImage != null,
            onClick = { onPreviousImage?.invoke() }
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_bold_left_24),
                contentDescription = "Previous image",
                tint = Color.Unspecified
            )
        }
        IconButton(
            enabled = onNextImage != null,
            onClick = { onNextImage?.invoke() }
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_bold_right_24),
                contentDescription = "Next image",
                tint = Color.Unspecified
            )
        }
        IconButton(onClick = onCurrentFootprint) {
            Icon(
                painter = painterResource(R.drawable.footprint_24),
                contentDescription = "Current footprint",
                tint = Color.Red
            )
        }
        IconButton(onClick = onAdditionalFootprints) {
            Icon(
                painter = painterResource(R.drawable.footprint_24),
                contentDescription = "Additional footprints",
                tint = Color.Blue
            )
        }
        IconButton(onClick = onAdditionalCameraLocations) {
            Icon(
                painter = painterResource(R.drawable.additional_cameras_24),
                contentDescription = "Additional camera locations",
                tint = Color.Unspecified
            )
        }
        IconToggleButton(
            checked = isSequentialNavigationEnabled,
            enabled = supportsSequentialNavigation,
            onCheckedChange = { onSequentialNavigation() }
        ) {
            Icon(
                painter = painterResource(R.drawable.sequential_navigation_24),
                contentDescription = "Location-image"
            )
        }
        IconButton(onClick = onReset) {
            Icon(
                painter = painterResource(R.drawable.reset_24),
                contentDescription = "Reset",
                tint = Color.Unspecified
            )
        }
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
