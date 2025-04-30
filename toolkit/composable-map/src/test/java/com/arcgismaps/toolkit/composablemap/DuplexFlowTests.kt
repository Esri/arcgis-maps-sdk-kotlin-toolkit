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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.junit.Test

@Suppress("DEPRECATION")
class DuplexFlowTests {

    @Test
    fun `test READ duplex`() {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Read

        assertThat(duplexFlow.getValue(flowType)).isNotNull()
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(0)

        duplexFlow.setValue(1, flowType)
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(1)

        duplexFlow.setValue(2, flowType)
        duplexFlow.setValue(3, flowType)
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(3)
    }

    @Test
    fun `test WRITE duplex`() {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Write

        assertThat(duplexFlow.getValue(flowType)).isNotNull()
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(0)

        duplexFlow.setValue(1, flowType)
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(1)

        duplexFlow.setValue(2, flowType)
        duplexFlow.setValue(3, flowType)
        assertThat(duplexFlow.getValue(flowType)).isEqualTo(3)
    }

    @Test
    fun `test collect on READ duplex`() = runBlocking {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Read
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            duplexFlow.collect(flowType) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            duplexFlow.setValue(i, flowType)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }

    @Test
    fun `test collect on WRITE duplex`() = runBlocking {
        val duplexFlow = MutableDuplexFlow(0)
        val flowType = DuplexFlow.Type.Write
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            duplexFlow.collect(flowType) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            duplexFlow.setValue(i, flowType)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }
}
