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

package com.arcgismaps.toolkit.utilitynetworks

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.toolkit.utilitynetworks.ui.SelectableItem
import com.arcgismaps.toolkit.utilitynetworks.ui.TraceOptions
import kotlinx.coroutines.launch

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
public val traceSurfaceContentDescription: String = "trace component surface"

/**
 * A composable UI component to set up and run a [com.arcgismaps.utilitynetworks.UtilityNetwork.trace]
 * on a [com.arcgismaps.toolkit.geoviewcompose.MapView].
 *
 * @since 200.6.0
 */
@Composable
public fun Trace(
    traceState: TraceState,
    @Suppress("unused_parameter")
    modifier: Modifier = Modifier
) {
    val configs = traceState.traceConfigurations.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = traceSurfaceContentDescription }
    ) {
        val scope = rememberCoroutineScope()

        if (configs.value == null) {
            Box(
                modifier = Modifier
                    .size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            TraceOptions(
                configurations = listOf(
                    SelectableItem(
                        "test trace configuration remove me",
                        false
                    )
                )
            ) {
                scope.launch {
                    traceState.trace()
                }
            }
        }
    }
}

