package com.arcgismaps.toolkit.composablemap

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.junit.Test

class MapFlowTests {

    @Test
    fun `test READ channel`() {
        val mapFlow = MutableMapFlow(0)
        assertThat(mapFlow.getValue(Channel.Read)).isNotNull()
        assertThat(mapFlow.getValue(Channel.Read)).isEqualTo(0)

        mapFlow.setValue(1, Channel.Read)
        assertThat(mapFlow.getValue(Channel.Read)).isEqualTo(1)

        mapFlow.setValue(2, Channel.Read)
        mapFlow.setValue(3, Channel.Read)
        assertThat(mapFlow.getValue(Channel.Read)).isEqualTo(3)
    }

    @Test
    fun `test WRITE channel`() {
        val mapFlow = MutableMapFlow(0)
        assertThat(mapFlow.getValue(Channel.Write)).isNotNull()
        assertThat(mapFlow.getValue(Channel.Write)).isEqualTo(0)

        mapFlow.setValue(1, Channel.Write)
        assertThat(mapFlow.getValue(Channel.Write)).isEqualTo(1)

        mapFlow.setValue(2, Channel.Write)
        mapFlow.setValue(3, Channel.Write)
        assertThat(mapFlow.getValue(Channel.Write)).isEqualTo(3)
    }

    @Test
    fun `test collect on READ channel`() = runBlocking {
        val mapFlow = MutableMapFlow(0)
        val channel = Channel.Read
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            mapFlow.collect(channel) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            mapFlow.setValue(i, channel)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }

    @Test
    fun `test collect on WRITE channel`() = runBlocking {
        val mapFlow = MutableMapFlow(0)
        val channel = Channel.Write
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            mapFlow.collect(channel) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            mapFlow.setValue(i, channel)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }
}
