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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.internal.util.TabRow
import com.arcgismaps.toolkit.utilitynetworks.ui.TraceNavHost

/**
 * A composable UI component to set up and run a [trace](https://developers.arcgis.com/kotlin/api-reference/arcgis-maps-kotlin/com.arcgismaps.utilitynetworks/-utility-network/trace.html)
 * workflow on a [com.arcgismaps.toolkit.geoviewcompose.MapView].
 *
 * @since 200.6.0
 */
@Stable
@Composable
public fun Trace(
    traceState: TraceState,
    modifier: Modifier = Modifier
) {
    val initializationStatus by traceState.initializationStatus
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(traceState) {
        traceState.initialize()
    }

    TraceScaffold(
        initializationStatus = initializationStatus,
        displayLinearProgressIndicator = traceState.isTaskInProgress.value,
        modifier = modifier,
        tabRow = if (traceState.showTabRow()) {
            {
                TabRow(
                    selectedTabIndex,
                    onNavigateTo = {
                        selectedTabIndex = it.first
                        traceState.showScreen(it.second)
                    }
                )
            }
        } else {
            null
        }
    ) {
        TraceNavHost(
            traceState,
            onTabSwitch = {
                selectedTabIndex = it
            }
        )
    }
}

@Composable
private fun TraceScaffold(
    initializationStatus: InitializationStatus,
    displayLinearProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
    tabRow: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val localContext = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = localContext.getString(R.string.trace_component) }
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (initializationStatus) {
                is InitializationStatus.NotInitialized, InitializationStatus.Initializing -> {
                    Box(
                        modifier = modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is InitializationStatus.FailedToInitialize -> {
                    val errorMessage = initializationStatus.error.getErrorMessage(localContext)
                    NonRecoveredErrorIndicator(errorMessage)
                    content()
                }

                else -> {
                    if (displayLinearProgressIndicator) {
                        LinearProgressIndicator()
                    }
                    tabRow?.invoke()
                    content()
                }
            }
        }
    }
}

@Composable
private fun NonRecoveredErrorIndicator(errorMessage: String) {
    Row {
        Icon(
            Icons.Default.Info,
            contentDescription = stringResource(id = R.string.error),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error
        )
    }
}

