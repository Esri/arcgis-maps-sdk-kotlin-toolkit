package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.composablemap.MapInterface
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapInterface, formViewModel: FeatureFormViewModelInterface) {
    val sheetVisibility = formViewModel.visible.collectAsState()
    
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    
    LaunchedEffect(sheetVisibility.value) {
        if (sheetVisibility.value) {
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
        sheetPeekHeight = 40.dp
    ) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            mapInterface = mapViewModel
        )
    }
}
