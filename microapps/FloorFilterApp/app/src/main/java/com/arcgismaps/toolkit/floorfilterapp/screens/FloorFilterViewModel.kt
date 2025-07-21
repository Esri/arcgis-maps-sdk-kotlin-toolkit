/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.arcgismaps.toolkit.floorfilterapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.indoors.FloorFilterSelection
import com.arcgismaps.toolkit.indoors.FloorFilterState
import kotlinx.coroutines.launch

class FloorFilterViewModel : ViewModel() {

    val floorAwareWebMap =
        ArcGISMap(
            PortalItem(
                Portal("https://arcgis.com/"),
                "b4b599a43a474d33946cf0df526426f5"
            )
        )

    val mapViewProxy = MapViewProxy()

    val floorFilterState: FloorFilterState =
        FloorFilterState(geoModel = floorAwareWebMap) { floorFilterSelection ->
            when (floorFilterSelection.type) {
                is FloorFilterSelection.Type.FloorSite -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorSite
                    floorFilterSelectionType.site.geometry?.let {
                        mapViewProxy.setViewpoint(Viewpoint(getEnvelopeWithBuffer(it)))
                    }
                }

                is FloorFilterSelection.Type.FloorFacility -> {
                    val floorFilterSelectionType =
                        floorFilterSelection.type as FloorFilterSelection.Type.FloorFacility
                    floorFilterSelectionType.facility.geometry?.let {
                        mapViewProxy.setViewpoint(Viewpoint(getEnvelopeWithBuffer(it)))
                    }
                }

                else -> {}
            }
        }

    /**
     * Returns the envelope with an added buffer factor applied to the given Geometry's extent.
     *
     * @since 200.2.0
     */
    private fun getEnvelopeWithBuffer(geometry: Geometry): Envelope {
        val bufferFactor = 1.25
        val envelope = geometry.extent
        return Envelope(envelope.center, envelope.width * bufferFactor, envelope.height * bufferFactor)
    }
}
