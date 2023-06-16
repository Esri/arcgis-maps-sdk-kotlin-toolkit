package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelFactory
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val coroutineScope = rememberCoroutineScope()
    val formViewModel = viewModel<FeatureFormViewModelImpl>(
        factory = FeatureFormViewModelFactory()
    )
    
    val mapViewModel = viewModel<FeatureFormsMapViewModelImpl>(
        factory = FeatureFormsMapViewModelFactory(
            arcGISMap = ArcGISMap("https://runtimecoretest.maps.arcgis.com/home/item.html?id=df0f27f83eee41b0afe4b6216f80b541"),
            onFeatureIdentified = { feature ->
                coroutineScope.launch {
                    formViewModel.setFeature(feature)
                    formViewModel.setFormVisibility(true)
                }
            }
        )
    )
    
    val inEditingMode by formViewModel.visible.collectAsState()
    
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            confirmValueChange = { it != SheetValue.Hidden },
            skipHiddenState = false
        )
    )
    
    LaunchedEffect(inEditingMode) {
        if (inEditingMode) {
            bottomSheetScaffoldState.bottomSheetState.expand()
        } else {
            bottomSheetScaffoldState.bottomSheetState.hide()
        }
    }
    
    BottomSheetScaffold(
        sheetContent = {
            FeatureForm(formViewModel)
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 40.dp,
        topBar = if (inEditingMode) {
            {
                TopAppBar(
                    title = { Text(text = "Edit Feature") },
                    actions = {
                        IconButton(onClick = {
                            /* FUTURE: save feature here */
                            formViewModel.setFormVisibility(false)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Feature",
                                tint = Color.Black
                            )
                        }
                        IconButton(onClick = {
                            formViewModel.setFormVisibility(false)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Feature Editor",
                                tint = Color.Black
                            )
                        }
                    }
                )
            }
        } else {
            null
        }
    ) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            mapInterface = mapViewModel
        )
    }
}

