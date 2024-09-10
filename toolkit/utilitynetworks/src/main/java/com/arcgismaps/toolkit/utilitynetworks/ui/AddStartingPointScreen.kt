/*
 *  Copyright 2024 Esri
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
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.utilitynetworks.AddStartingPointMode
import com.arcgismaps.toolkit.utilitynetworks.TraceState

/**
 * A composable screen that shows the UI to add a starting point for a trace.
 *
 * @since 200.6.0
 */
@Composable
internal fun AddStartingPointScreen(traceState: TraceState, onStopPointSelection: () -> Unit) {
    LaunchedEffect(key1 = traceState.addStartingPointMode) {
        traceState.addStartingPointMode.collect {
            if (it == AddStartingPointMode.Stop) {
                onStopPointSelection()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            Button(
                onClick = {
                    onStopPointSelection()
                    traceState.updateAddStartPointMode(AddStartingPointMode.Stop)
                }) {
                Text(text = "Cancel Starting Point Selection")
            }
        }
    }
}
