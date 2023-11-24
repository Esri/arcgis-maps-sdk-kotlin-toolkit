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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    val isStarted by featureEditorState.isStarted.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {

        map()

        Row {
            Button(
                onClick = { isBottomSheetVisible = !isBottomSheetVisible },
                enabled = isStarted
            ) { Text("Attributes/Geometry") }
            Button(
                onClick = { featureEditorState.featureEditor.stop() },
                enabled = isStarted
            ) { Text("Stop") }
            Button(
                onClick = { featureEditorState.featureEditor.discard() },
                enabled = isStarted
            ) { Text("Discard") }
        }


        if (isBottomSheetVisible) {
            ModalBottomSheet(onDismissRequest = { isBottomSheetVisible = false }) {
                FeatureForm(featureFormState = featureEditorState.featureFormState)
            }
        }
    }
}
