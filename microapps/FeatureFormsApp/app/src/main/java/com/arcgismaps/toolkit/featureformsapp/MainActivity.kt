package com.arcgismaps.toolkit.featureformsapp

import FeatureFormViewModelFactory
import FeatureFormViewModelImpl
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.composablemap.MapInsets
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelInterface
import com.arcgismaps.toolkit.featureformsapp.screens.mapview.FeatureFormsMapViewModelFactory
import com.arcgismaps.toolkit.featureformsapp.screens.mapview.FeatureFormsMapViewModelImpl
import com.arcgismaps.toolkit.featureformsapp.screens.mapview.FeatureFormsMapViewModelInterface
import com.arcgismaps.toolkit.featureformsapp.screens.mapview.MapScreen
import com.arcgismaps.toolkit.featureformsapp.ui.theme.FeatureFormsAppTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        
        val map = ArcGISMap(BasemapStyle.ArcGISChartedTerritory)
        
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val formViewModel = rememberFormViewModel()
            val mapViewModel = rememberMapViewModel(
                map = map,
                onFeatureIdentified = { feature ->
                    coroutineScope.launch {
                        formViewModel.feature.value = feature
                        formViewModel.visible.value = true
                    }
                }
            )
            
            FeatureFormsAppTheme {
                MapScreen(mapViewModel, formViewModel)
            }
        }
    }
}

@Composable
fun rememberFormViewModel(): FeatureFormViewModelInterface {
    return viewModel<FeatureFormViewModelImpl>(
        factory = FeatureFormViewModelFactory()
    )
}

@Composable
fun rememberMapViewModel(
    map: ArcGISMap,
    onFeatureIdentified: (ArcGISFeature) -> Unit
): FeatureFormsMapViewModelInterface {
    return viewModel<FeatureFormsMapViewModelImpl>(
        factory = FeatureFormsMapViewModelFactory(
            arcGISMap = map,
            mapInsets = MapInsets(bottom = 25.0),
            onFeatureIdentified = onFeatureIdentified
        )
    )
}