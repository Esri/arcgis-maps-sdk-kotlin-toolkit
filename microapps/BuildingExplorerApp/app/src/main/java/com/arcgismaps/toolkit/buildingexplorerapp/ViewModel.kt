/*
 COPYRIGHT 1995-2026 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

package com.arcgismaps.toolkit.buildingexplorerapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.toolkit.buildingexplorer.BuildingExplorerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModel(application: Application) : AndroidViewModel(application) {
    val scene =
        ArcGISScene("https://www.arcgis.com/home/item.html?id=b7c387d599a84a50aafaece5ca139d44")

    private val _showProgress = MutableStateFlow(true)
    val showProgress = _showProgress.asStateFlow()

    lateinit var buildingExplorerState: BuildingExplorerState

    init {
        viewModelScope.launch {
            scene.load()
                .onFailure { throw it }
                .onSuccess {
                    scene.operationalLayers.forEach { layer -> Log.d("LSV", "$layer") }

                    val buildingSceneLayer =
                        scene.operationalLayers.first { layer ->
                            layer is BuildingSceneLayer
                        } as BuildingSceneLayer

                    buildingExplorerState = BuildingExplorerState(
                        buildingSceneLayer = buildingSceneLayer,
                        coroutineScope = viewModelScope
                    )

                    _showProgress.value = false
                }
        }
    }
}