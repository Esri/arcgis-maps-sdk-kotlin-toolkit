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
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.ViewpointPersistence

@Composable
fun MapViewWithConfigurablePersistence(
    viewpointPersistence: ViewpointPersistence,
    onViewpointPersistenceSelected: (ViewpointPersistence) -> Unit,
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) },
    useSquareAspectRatio: Boolean = true
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
                viewpointPersistence = viewpointPersistence
            )
        }
    }
}

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