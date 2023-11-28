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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.EditingTransactionState
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

        Toolbar(
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

            Toolbar(
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
private fun Toolbar(
    onAttributeButtonPress: () -> Unit,
    attributeButtonState: AttributeButtonState,
    featureEditorState: FeatureEditorState
) {
    val isStarted by featureEditorState.isStarted.collectAsState()
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(2.dp)
            ) {
                if (attributeButtonState != AttributeButtonState.HIDE) {
                    val useGeometryIcon = attributeButtonState == AttributeButtonState.SHOW_GEOMETRY
                    Button(
                        onClick = onAttributeButtonPress,
                        enabled = isStarted,
                        shape = RectangleShape,
                        modifier = Modifier.padding(1.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (useGeometryIcon) R.drawable.baseline_edit_note_24 else R.drawable.baseline_notes_24
                            ),
                            contentDescription = if (useGeometryIcon) "Edit geometry" else "Edit attributes"
                        )
                    }
                }
                Button(
                    onClick = { featureEditorState.featureEditor.discard() },
                    enabled = isStarted,
                    shape = RectangleShape,
                    modifier = Modifier.padding(1.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_delete_forever_24),
                        contentDescription = "Discard"
                    )
                }
                Button(
                    onClick = { featureEditorState.featureEditor.stop() },
                    enabled = isStarted,
                    shape = RectangleShape,
                    modifier = Modifier.padding(1.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_check_circle_24),
                        contentDescription = "Stop"
                    )
                }
            }
        }
    }
}

private enum class AttributeButtonState { SHOW_ATTRIBUTES, SHOW_GEOMETRY, HIDE }
