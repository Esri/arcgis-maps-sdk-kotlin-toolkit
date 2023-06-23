package com.arcgismaps.toolkit.composablemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Defines the duplex channel type for a [MapFlow].
 */
public enum class Duplex {
    Read,
    Write
}

/**
 * A flow data structure that encapsulates two distinct flows. The READ flow channel is backed by
 * a [MutableStateFlow]. Only values that are intended to be read should be emitted/pushed to the
 * READ channel. The WRITE flow channel is backed by a [MutableSharedFlow] with a replay cache of 1.
 * Only values that are intended to be set should be pushed to the WRITE channel. There is no piping
 * mechanism that feeds across the channels, hence each flow is distinct.
 *
 * This provides a single data structure to be used with the [ComposableMap] and its [MapInterface]
 * to read property values using the READ channel and write property values using the WRITE channel.
 *
 * A [MapFlow] is read-only. See [MutableMapFlow] that provides a setter.
 *
 * Use the factory function MutableMapFlow(initialValue: T) to create an instance.
 */
public interface MapFlow<T> {

    /**
     * Returns the current value of the [duplex].
     */
    public fun getValue(duplex: Duplex) : T?

    /**
     * Accepts the given [collector] and emits values into it on the [duplex] provided.
     */
    public suspend fun collect(duplex: Duplex, collector: FlowCollector<T>)

    /**
     * Collects values from the [duplex] and its underlying flow and represents its latest value
     * via State.
     */
    @Composable
    public fun collectAsState(duplex: Duplex): State<T>
}

/**
 * A mutable [MapFlow] that provides a setter for the value of each channel.
 */
public interface MutableMapFlow<T> : MapFlow<T> {

    /**
     * Sets the current value for the [duplex].
     */
    public fun setValue(value: T, duplex: Duplex)
}

/**
 * Creates a [MutableMapFlow] with the given [initialValue]. The [initialValue] is set for
 * both channels.
 */
@Suppress("FunctionName")
public fun <T> MutableMapFlow(initialValue: T): MutableMapFlow<T> = MapFlowImpl(initialValue)

/**
 * Implementation for a [MutableMapFlow].
 */
internal class MapFlowImpl<T>(private val initialValue: T) : MutableMapFlow<T> {

    private val readerFlow: MutableStateFlow<T> = MutableStateFlow(initialValue)
    private val writerFlow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)

    init {
        writerFlow.tryEmit(initialValue)
    }

    override fun getValue(duplex: Duplex): T? {
        return if (duplex == Duplex.Read) {
            readerFlow.value
        } else {
            writerFlow.replayCache.firstOrNull()
        }
    }

    override suspend fun collect(duplex: Duplex, collector: FlowCollector<T>): Nothing =
        if (duplex == Duplex.Read) {
            coroutineScope { readerFlow.collect(collector) }
        } else {
            coroutineScope { writerFlow.collect(collector) }
        }

    @Composable
    override fun collectAsState(duplex: Duplex): State<T> =
        if (duplex == Duplex.Read) {
            readerFlow.collectAsState()
        } else {
            writerFlow.collectAsState(
                initial = writerFlow.replayCache.firstOrNull() ?: initialValue
            )
        }

    override fun setValue(value: T, duplex: Duplex) {
        if (duplex == Duplex.Read) {
            readerFlow.value = value
        } else {
            writerFlow.tryEmit(value)
        }
    }
}
