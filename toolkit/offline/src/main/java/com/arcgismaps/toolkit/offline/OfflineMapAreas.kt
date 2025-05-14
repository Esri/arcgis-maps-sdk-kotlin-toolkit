/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.offline

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
public fun OfflineMapAreas(
    offlineMapState: OfflineMapState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val initializationStatus by offlineMapState.initializationStatus

    LaunchedEffect(offlineMapState) {
        offlineMapState.initialize(scope, context)
    }

    Surface(
        modifier = modifier
    ) {
        when (initializationStatus) {
            is InitializationStatus.NotInitialized, InitializationStatus.Initializing -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is InitializationStatus.FailedToInitialize -> {
                (initializationStatus as InitializationStatus.FailedToInitialize).error.message?.let {
                    NonRecoveredErrorIndicator(
                        it
                    )
                }
            }

            else -> {
                if (offlineMapState.mode == OfflineMapMode.Preplanned) {
                    PreplannedMapAreas(
                        preplannedMapAreaStates = offlineMapState.preplannedMapAreaStates,
                        modifier = Modifier.clickable {
                            offlineMapState.takePreplannedMapOffline()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NonRecoveredErrorIndicator(errorMessage: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Icons.Default.Info,
            contentDescription = stringResource(id = R.string.error),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error
        )
    }
}
