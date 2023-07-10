/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.composablemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A flow data structure that encapsulates two distinct flows. This provides a single structure
 * that can be used to read property values using the [DuplexFlow.Type.Read] flow and write
 * property values using the [DuplexFlow.Type.Write] flow.
 *
 * The [DuplexFlow.Type.Read] flow is backed by a [MutableStateFlow]. Only values that are intended
 * to be read should be pushed to the READ flow. The [DuplexFlow.Type.Write] flow is
 * backed by a [MutableSharedFlow] with a replay cache of 1. Only values that are intended to be set
 * should be pushed to the WRITE flow. There is no piping mechanism that feeds across the
 * duplex, hence each flow is distinct.
 *
 * A [DuplexFlow] is read-only. See [MutableDuplexFlow] that provides a setter.
 *
 * Use the factory function MutableDuplexFlow(initialValue: T) to create an instance.
 */
public interface DuplexFlow<T> {

    /**
     * The duplex flow type for a [DuplexFlow].
     */
    public enum class Type {
        Read,
        Write
    }

    /**
     * Returns the current value of the [flowType].
     */
    public fun getValue(flowType: Type) : T?

    /**
     * Accepts the given [collector] and emits values into it on the [flowType] provided.
     */
    public suspend fun collect(flowType: Type, collector: FlowCollector<T>)

    /**
     * Collects values from the [flowType] and its underlying flow and represents its latest value
     * via State.
     */
    @Composable
    public fun collectAsState(flowType: Type): State<T>
}

/**
 * A mutable [DuplexFlow] that provides a setter for the value of each [DuplexFlow.Type].
 */
public interface MutableDuplexFlow<T> : DuplexFlow<T> {

    /**
     * Sets the current value for the [flowType].
     */
    public fun setValue(value: T, flowType: DuplexFlow.Type)
}

/**
 * Creates a [MutableDuplexFlow] with the given [initialValue]. The [initialValue] is set for
 * both the [DuplexFlow.Type]'s.
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

    override fun getValue(flowType: DuplexFlow.Type): T? {
        return if (flowType == DuplexFlow.Type.Read) {
            readerFlow.value
        } else {
            writerFlow.replayCache.firstOrNull()
        }
    }

    override suspend fun collect(flowType: DuplexFlow.Type, collector: FlowCollector<T>): Nothing =
        if (flowType == DuplexFlow.Type.Read) {
            coroutineScope { readerFlow.collect(collector) }
        } else {
            coroutineScope { writerFlow.collect(collector) }
        }

    @Composable
    override fun collectAsState(flowType: DuplexFlow.Type): State<T> =
        if (flowType == DuplexFlow.Type.Read) {
            readerFlow.collectAsState()
        } else {
            writerFlow.collectAsState(
                initial = writerFlow.replayCache.firstOrNull() ?: initialValue
            )
        }

    override fun setValue(value: T, flowType: DuplexFlow.Type) {
        if (flowType == DuplexFlow.Type.Read) {
            readerFlow.value = value
        } else {
            writerFlow.tryEmit(value)
        }
    }
}
