package com.arcgismaps.toolkit.composablemap

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.junit.Test

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
