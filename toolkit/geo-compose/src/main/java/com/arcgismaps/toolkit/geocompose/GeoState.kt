package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.DrawStatus
import kotlinx.coroutines.flow.MutableStateFlow

public sealed interface GeoComposeState {
    public val drawStatus: MutableStateFlow<DrawStatus?>
}

public fun GeoComposeState(): GeoComposeState = GeoComposeStateImpl()

public open class GeoComposeStateImpl() : GeoComposeState {
    override val drawStatus: MutableStateFlow<DrawStatus?> = MutableStateFlow(null)
}
