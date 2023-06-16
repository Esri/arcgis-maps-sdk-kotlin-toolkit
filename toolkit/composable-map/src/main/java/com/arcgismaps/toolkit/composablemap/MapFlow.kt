package com.arcgismaps.toolkit.composablemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

public enum class Channel {
    Read,
    Write
}

public interface MapFlow<T> {
    public suspend fun collect(channel: Channel, collector: FlowCollector<T>)
    @Composable
    public fun collectAsState(channel: Channel): State<T>
}

public interface MutableMapFlow<T> : MapFlow<T> {
    public fun setValue(value: T, channel: Channel)
}

public fun <T> MutableMapFlow(initialValue: T) : MutableMapFlow<T> = MapFlowImpl(initialValue)

internal open class MapFlowImpl<T>(private val initialValue: T) : MutableMapFlow<T> {

    private val readerFlow: MutableStateFlow<T> = MutableStateFlow(initialValue)
    private val writerFlow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)

    override suspend fun collect(channel: Channel, collector: FlowCollector<T>): Nothing =
        if (channel == Channel.Read) {
            coroutineScope { readerFlow.collect(collector) }
        } else {
            coroutineScope { writerFlow.collect(collector) }
        }

    @Composable
    override fun collectAsState(channel: Channel): State<T> =
        if (channel == Channel.Read) {
            readerFlow.collectAsState()
        } else {
            writerFlow.collectAsState(initial = initialValue)
        }

    override  fun setValue(value: T, channel: Channel) {
        if (channel == Channel.Read) {
            readerFlow.value = value
        } else {
            writerFlow.tryEmit(value)
        }
    }
}
