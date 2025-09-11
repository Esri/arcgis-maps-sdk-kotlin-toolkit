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
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

/**
 * Simple android view model for the Popup app map screen.
 */
class MapViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private var _geoElement: GeoElement? = null
    val geoElement: GeoElement?
        get() = _geoElement

    private var _layer: Layer? = null
    val layer: Layer?
        get() = _layer

    @Suppress("unused")
    private val fourteenersId = "9f3a674e998f461580006e626611f9ad"
    @Suppress("unused")
    private val ranchoId = "dd94764601554f1ea958f2d81906c698"
    @Suppress("unused")
    private val streamServiceMap = "aef32323d1f248368b1663cfc938995e"

    /**
     * The Popup read by the composition is held as a state variable.
     * We want the composition to recompose when the Popup changes.
     */
    private var _popup: MutableState<Popup?> = mutableStateOf(null)
    val popup: Popup?
        get() = _popup.value

    val map = ArcGISMap(
        PortalItem(
            Portal.arcGISOnline(Portal.Connection.Authenticated),
            fourteenersId
        )
    ).apply {
        Viewpoint(40.559691, -111.869001, 150000.0)
    }

    val proxy: MapViewProxy = MapViewProxy()

    init {
        viewModelScope.launch {
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
}

