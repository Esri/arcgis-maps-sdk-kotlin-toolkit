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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.FeatureForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun FeatureEditorView(
    // TODO: name?
    featureEditorState: FeatureEditorState,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        map()

        Toolbar(
            onAttributeButtonPress = { isBottomSheetVisible = !isBottomSheetVisible },
            featureEditorState = featureEditorState
        )

        if (isBottomSheetVisible) {
            ModalBottomSheet(onDismissRequest = { isBottomSheetVisible = false }) {
                FeatureForm(featureFormState = featureEditorState.featureFormState)
            }
        }
    }
}

@Composable
private fun Toolbar(onAttributeButtonPress: () -> Unit, featureEditorState: FeatureEditorState) {
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
                Button(
                    onClick = onAttributeButtonPress,
                    enabled = isStarted,
                    shape = RectangleShape,
                    modifier = Modifier.padding(1.dp),
                ) { Text("Attr / Geom") }
                Button(
                    onClick = { featureEditorState.featureEditor.stop() },
                    enabled = isStarted,
                    shape = RectangleShape,
                    modifier = Modifier.padding(1.dp),
                ) { Text("Stop") }
                Button(
                    onClick = { featureEditorState.featureEditor.discard() },
                    enabled = isStarted,
                    shape = RectangleShape,
                    modifier = Modifier.padding(1.dp),
                ) { Text("Discard") }
            }
        }
    }
}
