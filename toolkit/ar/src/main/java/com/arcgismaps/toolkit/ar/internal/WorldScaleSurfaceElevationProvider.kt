package com.arcgismaps.toolkit.ar.internal

import android.util.Log
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WorldScaleSurfaceElevationProvider(private val surface: Surface, private val scope: CoroutineScope) {

    private val _elevation: MutableStateFlow<ElevationInfo?> = MutableStateFlow(null)
    val elevation: StateFlow<ElevationInfo?> = _elevation.asStateFlow()

    fun obtainElevation(location: Point) {
        scope.launch(Dispatchers.IO) {
            // make sure the surface is loaded before getting the elevation
            surface.load().onSuccess {
                surface.getElevation(location).onSuccess {
                    Log.d("WorldScaleSurfaceElevationProvider", "Elevation: $it")
                    _elevation.value = ElevationInfo(location, it)
                }.onFailure {
                    Log.d("WorldScaleSurfaceElevationProvider", "Elevation failed: ${it.message}")
                }
            }
        }
    }

    internal data class ElevationInfo(val location: Point, val elevation: Double)
}
