package com.arcgismaps.toolkit.composablemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Defines the duplex channel type for a [DuplexFlow].
 */
public enum class Duplex {
    Read,
    Write
}

/**
 * A flow data structure that encapsulates two distinct flows. This provides a single structure
 * that can be used to read property values using the [Duplex.Read] channel and write property
 * values using the [Duplex.Write] channel.
 *
 * The [Duplex.Read] flow is backed by a [MutableStateFlow]. Only values that are intended to be
 * read should be emitted/pushed to the READ duplex channel. The [Duplex.Write] flow is backed by
 * a [MutableSharedFlow] with a replay cache of 1. Only values that are intended to be set should
 * be pushed to the WRITE duplex channel. There is no piping mechanism that feeds across the duplex,
 * hence each flow is distinct.
 *
 * A [DuplexFlow] is read-only. See [MutableDuplexFlow] that provides a setter.
 *
 * Use the factory function MutableDuplexFlow(initialValue: T) to create an instance.
 */
public interface DuplexFlow<T> {

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
 * A mutable [DuplexFlow] that provides a setter for the value of each channel.
 */
public interface MutableDuplexFlow<T> : DuplexFlow<T> {

    /**
     * Sets the current value for the [duplex].
     */
    public fun setValue(value: T, duplex: Duplex)
}

/**
 * Creates a [MutableDuplexFlow] with the given [initialValue]. The [initialValue] is set for
 * both duplex channels.
 */
@Suppress("FunctionName")
public fun <T> MutableDuplexFlow(initialValue: T): MutableDuplexFlow<T> = DuplexFlowImpl(initialValue)

/**
 * Implementation for a [MutableDuplexFlow].
 */
internal class DuplexFlowImpl<T>(private val initialValue: T) : MutableDuplexFlow<T> {

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
