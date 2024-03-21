package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.ViewpointPersistence

/**
 * Shows a MapView with an option of the viewpoint persistence type above it.
 *
 * @param viewpointPersistence the viewpoint persistence state to use, use rememberSaveable to preserve the state across configuration changes
 * @param onViewpointPersistenceSelected the callback to be invoked when the viewpoint persistence type is selected
 * @param modifier the modifier to be applied to the MapView
 * @param arcGISMap the ArcGISMap to be used in the MapView
 * @param useSquareAspectRatio whether to use a square aspect ratio for the MapView
 */
@Composable
fun MapViewWithConfigurablePersistence(
    viewpointPersistence: ViewpointPersistence,
    onViewpointPersistenceSelected: (ViewpointPersistence) -> Unit,
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    useSquareAspectRatio: Boolean = true,
    mapViewProxy: MapViewProxy? = null
) {
    Surface(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewpointPersistenceSelectionRow(viewpointPersistence, onViewpointPersistenceSelected)
            MapView(
                arcGISMap = arcGISMap,
                modifier = if (useSquareAspectRatio) Modifier.aspectRatio(1.0f) else Modifier.fillMaxSize(),
                viewpointPersistence = viewpointPersistence,
                mapViewProxy = mapViewProxy
            )
        }
    }
}

/**
 * Displays the currently selected viewpoint persistence and allows the user to change it.
 */
@Composable
private fun ViewpointPersistenceSelectionRow(
    viewpointPersistence: ViewpointPersistence,
    onViewpointPersistenceSelected: (ViewpointPersistence) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        IconButton(onClick = {
            isExpanded = true
        }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Viewpoint Persistence Options"
            )
        }
        val text = viewpointPersistence.javaClass.simpleName
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        ViewpointPersistenceOptions(
            isExpanded,
            onDismiss = { isExpanded = false },
            onSelected = {
                val selectedViewpointPersistence = when (it) {
                    "None" -> ViewpointPersistence.None
                    "ByCenterAndScale" -> ViewpointPersistence.ByCenterAndScale()
                    "ByBoundingGeometry" -> ViewpointPersistence.ByBoundingGeometry()
                    else -> throw IllegalArgumentException("Unknown viewpoint persistence type: $it")
                }
                onViewpointPersistenceSelected(selectedViewpointPersistence)
            }
        )
    }
}

/**
 * Displays the options for viewpoint persistence types.
 */
@Composable
fun ViewpointPersistenceOptions(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) = DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
    viewpointPersistenceTypes.forEach { persistence ->
        DropdownMenuItem(
            text = { Text(persistence) },
            onClick = {
                onSelected(persistence)
                onDismiss()
            }
        )
    }
}

private val viewpointPersistenceTypes = listOf("None", "ByCenterAndScale", "ByBoundingGeometry")