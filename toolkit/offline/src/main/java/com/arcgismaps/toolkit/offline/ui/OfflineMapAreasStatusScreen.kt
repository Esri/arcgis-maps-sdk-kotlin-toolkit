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

package com.arcgismaps.toolkit.offline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.R

@Composable
internal fun OfflineMapAreasStatusContent(
    title: String,
    message: String,
    icon: ImageVector,
    onlyFooterVisible: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onlyFooterVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    modifier = Modifier.wrapContentSize(),
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

@Composable
internal fun RefreshButton(onRefresh: () -> Unit) {
    Button(onClick = onRefresh) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Default.Refresh,
            contentDescription = "A label for a button to refresh map area content."
        )
        Spacer(Modifier.width(4.dp))
        Text(text = stringResource(R.string.refresh), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun NoInternetNoAreas(onlyFooterVisible: Boolean = false, onRefresh: () -> Unit) {
    OfflineMapAreasStatusContent(
        title = stringResource(R.string.no_internet_connection_error_message),
        message = stringResource(R.string.no_internet_error_message),
        icon = Icons.Default.WifiOff,
        actions = { RefreshButton(onRefresh) },
        onlyFooterVisible = onlyFooterVisible
    )
}

@Composable
internal fun EmptyPreplannedOfflineAreas(
    onlyFooterVisible: Boolean = false,
    onRefresh: () -> Unit
) {
    OfflineMapAreasStatusContent(
        title = stringResource(R.string.no_map_areas),
        message = stringResource(R.string.no_offline_map_areas_error_message),
        icon = Icons.Default.ArrowDownward,
        actions = { RefreshButton(onRefresh) },
        onlyFooterVisible = onlyFooterVisible
    )
}

@Composable
internal fun OfflineMapAreasError(
    error: Throwable,
    onlyFooterVisible: Boolean = false,
    onRefresh: () -> Unit
) {
    OfflineMapAreasStatusContent(
        title = stringResource(R.string.error_fetching_areas),
        message = stringResource(R.string.error_fetching_areas_message) + "\n\n" + error.message,
        icon = Icons.Default.Error,
        actions = { RefreshButton(onRefresh) },
        onlyFooterVisible = onlyFooterVisible
    )
}

@Composable
internal fun OfflineDisabled(onlyFooterVisible: Boolean = false, onRefresh: () -> Unit) {
    OfflineMapAreasStatusContent(
        title = stringResource(R.string.offline_disabled),
        message = stringResource(R.string.offline_disabled_message),
        icon = Icons.Default.Block,
        actions = { RefreshButton(onRefresh) },
        onlyFooterVisible = onlyFooterVisible
    )
}

@Composable
internal fun EmptyOnDemandOfflineAreas(onlyFooterVisible: Boolean = false, onAdd: () -> Unit) {
    OfflineMapAreasStatusContent(
        title = stringResource(R.string.no_map_areas),
        message = stringResource(R.string.empty_on_demand_message),
        icon = Icons.Default.ArrowDownward,
        actions = { Button(onClick = onAdd) { Text(stringResource(R.string.map_areas)) } },
        onlyFooterVisible = onlyFooterVisible
    )
}

@Preview(showBackground = true)
@Composable
private fun NoInternetNoAreasPreview() {
    MaterialTheme {
        Surface {
            NoInternetNoAreas { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoInternetNoAreasFooterPreview() {
    MaterialTheme {
        Surface {
            NoInternetNoAreas(onlyFooterVisible = true) { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreplannedOfflineAreasPreview() {
    MaterialTheme {
        Surface {
            EmptyPreplannedOfflineAreas { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineMapAreasErrorPreview() {
    MaterialTheme {
        Surface {
            OfflineMapAreasError(error = Throwable("Failed to initialize map areas")) { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineDisabledPreview() {
    MaterialTheme {
        Surface {
            OfflineDisabled { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyOnDemandOfflineAreasPreview() {
    MaterialTheme {
        Surface {
            EmptyOnDemandOfflineAreas { }
        }
    }
}
