/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popupapp.screens.mapscreen

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.coroutines.CoroutineContext

/**
 * Base class for context aware AndroidViewModel. This class must have only a single application
 * parameter.
 */
open class BaseMapViewModel(application: Application) : AndroidViewModel(application)

/**
 * Simple android view model for the Popup app map screen.
 */
@Suppress("unused_parameter")
class MapViewModel(
    savedStateHandle: SavedStateHandle,
    application: Application,
    coroutineScope: CoroutineScope = CloseableCoroutineScope()
) : BaseMapViewModel(application) {

    private var _geoElement: GeoElement? = null
    val geoElement: GeoElement?
        get() = _geoElement

    private var _layer: Layer? = null
    val layer: Layer?
        get() = _layer

    /**
     * The Popup read by the composition is held as a state variable.
     * We want the composition to recompose when the Popup changes.
     */
    private var _popup: MutableState<Popup?> = mutableStateOf(null)
    val popup: Popup?
        get() = _popup.value

    val map = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(Portal.Connection.Anonymous),
            "9f3a674e998f461580006e626611f9ad"
        )
    ).apply {
        initialViewpoint = Viewpoint(
            latitude = 34.052235,
            longitude = -118.243683,
            scale = 10e6
        )
    }

    val proxy: MapViewProxy = MapViewProxy()

    init {
        coroutineScope.launch {
            map.load()
        }
    }

    fun setGeoElement(element: GeoElement?) {
        _geoElement = element
    }

    fun setLayer(layer: Layer?) {
        _layer = layer
    }

    fun setPopup(popup: Popup?) {
        _popup.value = popup
    }
    companion object {

        /**
         * The factory needed by the androidx ktx component activity to instantiate the view model.
         * See onCreate() for usage.
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()

                return MapViewModel(
                    savedStateHandle,
                    application
                ) as T
            }
        }
    }

}

/**
 * a CoroutineScope used by the view model. It will by closed when the view model exits its
 * lifecycle.
 */
class CloseableCoroutineScope(
    context: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate
) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context
    override fun close() {
        coroutineContext.cancel()
    }
}
