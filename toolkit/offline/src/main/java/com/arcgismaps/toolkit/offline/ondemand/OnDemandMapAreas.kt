package com.arcgismaps.toolkit.offline.ondemand

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a list of on-demand map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun OnDemandMapAreas(
    onDemandMapAreasStates: List<OnDemandMapAreasState>,
    modifier: Modifier
) {
    Column {
        onDemandMapAreasStates.forEach { state ->
            Text(state.title)
        }
    }
}
