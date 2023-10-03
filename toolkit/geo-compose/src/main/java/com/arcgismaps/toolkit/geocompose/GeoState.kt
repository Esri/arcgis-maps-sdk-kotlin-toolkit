package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public sealed interface GeoState {
    public fun registerGeoView(geoView: GeoView)
    public fun unregisterGeoView()

    public var geoView: MutableStateFlow<GeoView?>
    public val drawStatus: StateFlow<DrawStatus?>
}

public fun GeoState(coroutineScope: CoroutineScope): GeoState =
    GeoStateImpl(coroutineScope)

public open class GeoStateImpl(coroutineScope: CoroutineScope) : GeoState {
    private val _drawStatus: MutableStateFlow<DrawStatus?> = MutableStateFlow(null)
    override val drawStatus: StateFlow<DrawStatus?> = _drawStatus.asStateFlow()

    override var geoView: MutableStateFlow<GeoView?> = MutableStateFlow(null)

    public override fun registerGeoView(geoView: GeoView) {
        check(this.geoView.value == null) { "MapState is already used in a composition" }
        this.geoView.value = geoView
    }

    public override fun unregisterGeoView() {
        geoView.value = null
    }

    init {
        coroutineScope.launch {
            geoView.value?.drawStatus?.collect {
                _drawStatus.value = it
            }
        }
    }
}
