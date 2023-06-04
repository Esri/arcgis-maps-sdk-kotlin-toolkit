package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelFactory
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic).also {
        it.initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    
    val formViewModel = viewModel<FeatureFormViewModelImpl>(
        factory = FeatureFormViewModelFactory()
    )
    
    val mapViewModel = viewModel<FeatureFormsMapViewModelImpl>(
        factory = FeatureFormsMapViewModelFactory(
            arcGISMap = map,
            mapInsets = MapInsets(bottom = 25.0),
            onFeatureIdentified = { feature ->
                coroutineScope.launch {
                    formViewModel.feature.value = feature
                    if (bottomSheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                        bottomSheetScaffoldState.bottomSheetState.expand()
                    }
                }
            }
        )
    )
    
    BottomSheetScaffold(
        sheetContent = {
            FeatureForm(formViewModel)
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 40.dp
    ) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            mapInterface = mapViewModel
        )
    }
}
