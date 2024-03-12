package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geocompose.ViewpointPersistence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC2TwoMapViews() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC2: Two MapViews") })
        }
    ) {
        var viewpointPersistenceTop: ViewpointPersistence by rememberSaveable { mutableStateOf(
            ViewpointPersistence.ByCenterAndScale()) }
        var viewpointPersistenceBottom: ViewpointPersistence by rememberSaveable { mutableStateOf(
            ViewpointPersistence.ByBoundingGeometry()) }

        val content : @Composable (Modifier) -> Unit = @Composable {
            key (Unit) {
                MapViewWithConfigurablePersistence(
                    viewpointPersistence = viewpointPersistenceTop,
                    onViewpointPersistenceSelected = { viewpointPersistenceTop = it },
                    modifier = it,
                    arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }
                )

                MapViewWithConfigurablePersistence(
                    viewpointPersistence = viewpointPersistenceBottom,
                    onViewpointPersistenceSelected = { viewpointPersistenceBottom = it },
                    modifier = it,
                    arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISTopographic) }
                )
            }
        }
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(modifier = Modifier.padding(it), verticalArrangement = Arrangement.SpaceEvenly) {
                content(Modifier.weight(0.5f).fillMaxWidth())
            }
        } else {
            Row(modifier = Modifier.padding(it), horizontalArrangement = Arrangement.SpaceEvenly) {
                content(Modifier.weight(0.5f))
            }
        }
    }
}