package com.arcgismaps.toolkit.utilitynetworktraceapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.ServiceGeodatabase
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.ArcGISVectorTiledLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.utilitynetworks.TraceState
import com.arcgismaps.toolkit.utilitynetworktraceapp.BuildConfig
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TraceViewModel : ViewModel() {

    private val napervilleUtilities = "471eb0bf37074b1fbb972b1da70fb310"

//    val arcGISMap = ArcGISMap(
//        PortalItem(
//            Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
//            napervilleUtilities
//        )
//    )

    val basemapLayer =
        ArcGISVectorTiledLayer("https://www.arcgis.com/home/item.html?id=86f556a2d1fd468181855a35e344567f")
    var arcGISMap: MutableStateFlow<ArcGISMap> = MutableStateFlow(ArcGISMap())

    val mapViewProxy = MapViewProxy()

    val graphicsOverlay = GraphicsOverlay()

    val traceState: MutableStateFlow<TraceState> = MutableStateFlow(TraceState(arcGISMap.value, graphicsOverlay, mapViewProxy))

    init {
        viewModelScope.launch {
            val tokenCred =
                TokenCredential.create(
                    "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
                    username = BuildConfig.traceToolUser,
                    password = BuildConfig.traceToolPassword
                ).getOrThrow()
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCred)

            val mapWithUn = ArcGISMap(SpatialReference.webMercator()).apply {
                setBasemap(Basemap(basemapLayer))
            }

            var parentUrl = "https://sampleserver7.arcgisonline.com/server/rest/services/UtilityNetwork/NapervilleElectricV5/FeatureServer";
            var serviceGeodatabase = ServiceGeodatabase(parentUrl)
            serviceGeodatabase.load().onSuccess {
                // Add the UN to the map BEFORE loading it — the map must own it
                // and only unloaded UtilityNetworks can be added to Map.UtilityNetworks
                val un = UtilityNetwork(serviceGeodatabase);

                //MainThread.BeginInvokeOnMainThread(() => MyMapView.Map.UtilityNetworks.Add(un));
                mapWithUn.utilityNetworks.add(un);

                //FOR SOME REASON ALL THE LAYERS FROM THE SERVICEGEODATABASE WONT LOAD ON THE MAP
                serviceGeodatabase.serviceInfo?.layerInfos?.forEach {
                    val serviceFeatureTable = serviceGeodatabase.getTable(it.id ?: -1)
                    val subTypeFeatureLayer = SubtypeFeatureLayer(serviceFeatureTable!!)
                    mapWithUn.operationalLayers.add(subTypeFeatureLayer);

                }
                arcGISMap.value = mapWithUn
                traceState.value = TraceState(mapWithUn, graphicsOverlay, mapViewProxy)
            }.onFailure {
                Log.d("TraceViewModel", "Failed to load service geodatabase: ${it.message}")
            }
        }
    }
}
