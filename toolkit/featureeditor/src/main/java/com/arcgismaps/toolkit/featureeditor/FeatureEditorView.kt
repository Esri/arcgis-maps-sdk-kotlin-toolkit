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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Box(modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)) {
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
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Surface(shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {

                ToolbarButton(
                    onClick = { showGeometryButtonGroup = !showGeometryButtonGroup },
                    enabled = isStarted,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_swap_vert_24),
                        contentDescription = "Swap toolbar"
                    )
                }

                Divider(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .padding(1.dp)
                )

                if (showGeometryButtonGroup && isStarted) GeometryButtonGroup(
                    featureEditorState = featureEditorState,
                    attributeButtonState = attributeButtonState,
                ) else ControlButtonGroup(
                    isStarted = isStarted,
                    onAttributeButtonPress = onAttributeButtonPress,
                    attributeButtonState = attributeButtonState,
                    featureEditorState = featureEditorState,
                )
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

    if (attributeButtonState != AttributeButtonState.HIDE) {
        ToolbarButton(
            onClick = {},
            enabled = false
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_local_hotel_24),
                contentDescription =
                "Local hotel we need to have the same number of buttons in both toolbars or else they don't align"
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        modifier = Modifier.padding(1.dp),
        contentPadding = contentPadding,
        content = content,
    )
}

private enum class AttributeButtonState { SHOW_ATTRIBUTES, SHOW_GEOMETRY, HIDE }
