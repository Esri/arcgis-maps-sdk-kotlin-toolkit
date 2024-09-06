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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = traceSurfaceContentDescription }
    ) {
        val configurations = traceState.traceConfigurations.collectAsStateWithLifecycle()
        if (configurations.value != null) {
            Column {
                Text(
                    text = traceState.traceResult.collectAsStateWithLifecycle().value?.toString()
                        ?: "Null"
                )
                Button(onClick = { traceState.trace() }) {
                    Text(stringResource(id = R.string.trace))
                }
            }
        } else {
            Box {
                CircularProgressIndicator()
            }
        }
    }
}

