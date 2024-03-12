package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.geocompose.ViewpointPersistence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC1MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC1: Single MapView") })
        }
    ) {
        var viewpointPersistence: ViewpointPersistence by rememberSaveable { mutableStateOf(ViewpointPersistence.ByCenterAndScale()) }

        MapViewWithConfigurablePersistence(
            viewpointPersistence = viewpointPersistence,
            onViewpointPersistenceSelected = { viewpointPersistence = it },
            modifier = Modifier.padding(it)
        )
    }
}