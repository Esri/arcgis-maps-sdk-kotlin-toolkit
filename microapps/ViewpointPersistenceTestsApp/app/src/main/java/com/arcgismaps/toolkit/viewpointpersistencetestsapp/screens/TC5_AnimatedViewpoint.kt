package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.ViewpointPersistence
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC5AnimatedViewpoint() {
    val mapViewProxy = remember { MapViewProxy() }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "TC5: Animated Viewpoint") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            mapViewProxy.setViewpointAnimated(
                                Viewpoint(43.5, 13.5, 20000000.0), 30.0.seconds
                            )
                        }
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Animate to initial viewpoint"
                        )
                    }
                }
            )
        },
    ) {
        var viewpointPersistence: ViewpointPersistence by rememberSaveable {
            mutableStateOf(
                ViewpointPersistence.ByCenterAndScale()
            )
        }
        MapViewWithConfigurablePersistence(
            viewpointPersistence = viewpointPersistence,
            onViewpointPersistenceSelected = { viewpointPersistence = it },
            modifier = Modifier.padding(it),
            mapViewProxy = mapViewProxy
        )
    }
}