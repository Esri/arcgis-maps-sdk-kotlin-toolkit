package com.arcgismaps.toolkit.composablemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

public open class MapFlow<T>(
    private val initialValue: T
) {

    public enum class Channel {
        Read,
        Write
    }

    protected val readerFlow: MutableStateFlow<T> = MutableStateFlow(initialValue)
    protected val writerFlow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)

    public suspend fun collect(channel: Channel, collector: FlowCollector<T>): Nothing =
        if (channel == Channel.Read) {
            coroutineScope { readerFlow.collect(collector) }
        } else {
            coroutineScope { writerFlow.collect(collector) }
        }

    @Composable
    public fun collectAsState(channel: Channel): State<T> =
        if (channel == Channel.Read) {
            readerFlow.collectAsState()
        } else {
            writerFlow.collectAsState(initial = initialValue)
        }
}

public class MutableMapFlow<T>(
    initialValue: T
) : MapFlow<T>(initialValue) {
    public fun setValue(value: T, channel: Channel) {
        if (channel == Channel.Read) {
            readerFlow.value = value
        } else {
            writerFlow.tryEmit(value)
        }
    }
}
