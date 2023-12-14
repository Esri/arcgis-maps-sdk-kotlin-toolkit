/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureeditor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.FeatureForm

// TODO: should name contain "view"?
@Composable
public fun FeatureEditorView(
    featureEditorState: FeatureEditorState,
    modifier: Modifier = Modifier,
    useSideBySideView: Boolean = false,
    map: @Composable () -> Unit,
) {
    if (useSideBySideView) SideBySideFeatureEditorView(
        featureEditorState = featureEditorState,
        modifier = modifier,
        map = map,
    ) else CompactFeatureEditorView(
        featureEditorState = featureEditorState,
        modifier = modifier,
        map = map,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFeatureEditorView(
    featureEditorState: FeatureEditorState,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        map()

        FeatureEditorToolbar(
            onAttributeButtonPress = { isBottomSheetVisible = !isBottomSheetVisible },
            featureEditorState = featureEditorState,
            attributeButtonState =
            if (isBottomSheetVisible) AttributeButtonState.SHOW_GEOMETRY else AttributeButtonState.SHOW_ATTRIBUTES,
        )

        if (isBottomSheetVisible) {
            ModalBottomSheet(onDismissRequest = { isBottomSheetVisible = false }) {
                FeatureForm(featureFormState = featureEditorState.featureFormState)
            }
        }
    }
}

@Composable
private fun SideBySideFeatureEditorView(
    featureEditorState: FeatureEditorState,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    val isStarted by featureEditorState.isStarted.collectAsState()
    Row(modifier = modifier) { // TODO: is it correct to use the incoming modifier here and nowhere else?
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
        ) {
            map()

            FeatureEditorToolbar(
                onAttributeButtonPress = {},
                featureEditorState = featureEditorState,
                attributeButtonState = AttributeButtonState.HIDE,
            )
        }

        if (isStarted) FeatureForm(
            featureFormState = featureEditorState.featureFormState,
            modifier = Modifier.fillMaxSize()
        ) else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Select a feature to begin editing!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureEditorToolbar(
    onAttributeButtonPress: () -> Unit,
    attributeButtonState: AttributeButtonState,
    featureEditorState: FeatureEditorState
) {

    var showGeometryButtonGroup by remember { mutableStateOf(false) }
    val isStarted by featureEditorState.isStarted.collectAsState()
    // This first row creates a space at the top of the screen where content can be centered.
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // This surface creates the background for whatever content occupying the space. It doesn't necessarily
        // fill the whole space defined above because we haven't used `fillMaxWidth`.
        Surface(shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)) {
            // This second row lets just place items horizontally within the surface.
            Row(
                modifier = Modifier.padding(2.dp)
            ) {
                ToolbarButton(
                    onClick = { showGeometryButtonGroup = !showGeometryButtonGroup },
                    enabled = isStarted,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.padding(top = 1.dp, start = 1.dp, bottom = 1.dp, end = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_swap_vert_24),
                        contentDescription = "Swap toolbar"
                    )
                }

                // Here we are using SubcomposeLayout so that we can measure the control toolbar
                // and use that to set the width of the geometry toolbar, so that they take up the same
                // area of the screen, so there's no moving around when switching between them.
                SubcomposeLayout { constraints ->
                    val controlPlaceables = subcompose(0) {
                        Row {
                            ControlButtonGroup(
                                isStarted = isStarted,
                                onAttributeButtonPress = onAttributeButtonPress,
                                attributeButtonState = attributeButtonState,
                                featureEditorState = featureEditorState,
                            )
                        }
                    }.map { it.measure(Constraints()) }

                    // The control placeables define the width the geometry placeables -- any larger
                    // and the geometry placeables will scroll to accommodate.
                    // The control placeables and the geometry placeables are assumed to have the same height.
                    // Therefore we only look at the control placeables when deciding the dimensions of the component.
                    val width = controlPlaceables.maxBy { it.width }.width
                    val height = controlPlaceables.maxBy { it.height }.height

                    val geometryPlaceables = subcompose(1) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            GeometryButtonGroup(
                                featureEditorState = featureEditorState,
                                attributeButtonState = attributeButtonState,
                            )
                        }
                    }.map { it.measure(Constraints(maxWidth = width)) }

                    if (showGeometryButtonGroup && isStarted) {
                        layout(width, height) {
                            // Note we're expecting there only to be one placeable.
                            geometryPlaceables.first().placeRelative(0, 0)
                        }
                    } else {
                        layout(width, height) {
                            controlPlaceables.first().placeRelative(0, 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButtonGroup(
    isStarted: Boolean,
    onAttributeButtonPress: () -> Unit,
    attributeButtonState: AttributeButtonState,
    featureEditorState: FeatureEditorState,
) {
    if (attributeButtonState != AttributeButtonState.HIDE) {
        val useGeometryIcon = attributeButtonState == AttributeButtonState.SHOW_GEOMETRY
        ToolbarButton(
            onClick = onAttributeButtonPress,
            enabled = isStarted,
        ) {
            Icon(
                painter = painterResource(
                    id = if (useGeometryIcon) R.drawable.baseline_edit_24 else R.drawable.baseline_edit_note_24
                ),
                contentDescription = if (useGeometryIcon) "Edit geometry" else "Edit attributes"
            )
        }
    }
    ToolbarButton(
        onClick = { featureEditorState.featureEditor.discard() },
        enabled = isStarted,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_delete_forever_24),
            contentDescription = "Discard"
        )
    }
    ToolbarButton(
        onClick = { featureEditorState.featureEditor.stop() },
        enabled = isStarted,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_check_circle_24),
            contentDescription = "Stop"
        )
    }
}

@Composable
private fun GeometryButtonGroup(
    featureEditorState: FeatureEditorState,
    attributeButtonState: AttributeButtonState,
) {
    ToolbarButton(
        onClick = { featureEditorState.featureEditor.geometryEditor.undo() },
        enabled = featureEditorState.featureEditor.geometryEditor.canUndo.collectAsState().value
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_undo_24),
            contentDescription = "Undo"
        )
    }

    ToolbarButton(
        onClick = { featureEditorState.featureEditor.geometryEditor.redo() },
        enabled = featureEditorState.featureEditor.geometryEditor.canRedo.collectAsState().value
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_redo_24),
            contentDescription = "Redo"
        )
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier.padding(1.dp),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        modifier = modifier.padding(1.dp),
        contentPadding = contentPadding,
        content = content,
    )
}

private enum class AttributeButtonState { SHOW_ATTRIBUTES, SHOW_GEOMETRY, HIDE }
