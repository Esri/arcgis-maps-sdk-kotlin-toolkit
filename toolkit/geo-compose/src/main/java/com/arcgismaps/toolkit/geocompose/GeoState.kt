package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.Callout
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public sealed interface GeoState {
    public val callout: StateFlow<Callout?>
    public val drawStatus: StateFlow<DrawStatus?>
}

public fun GeoState(geoView: GeoView, coroutineScope: CoroutineScope): GeoState =
    GeoStateImpl(geoView, coroutineScope)

public open class GeoStateImpl(geoView: GeoView, coroutineScope: CoroutineScope) : GeoState {
    private val _callout: MutableStateFlow<Callout?> = MutableStateFlow(null)
    override val callout: StateFlow<Callout?> = _callout.asStateFlow()

    private val _drawStatus: MutableStateFlow<DrawStatus?> = MutableStateFlow(null)
    override val drawStatus: StateFlow<DrawStatus?> = _drawStatus.asStateFlow()

    init {
        coroutineScope.launch {
            geoView.drawStatus.collect{
                _drawStatus.value = it
            }
        }
    }

}
