package com.arcgismaps.toolkit.composablemap

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.junit.Test

class MapFlowTests {

    @Test
    fun `test READ duplex`() {
        val mapFlow = MutableMapFlow(0)
        assertThat(mapFlow.getValue(Duplex.Read)).isNotNull()
        assertThat(mapFlow.getValue(Duplex.Read)).isEqualTo(0)

        mapFlow.setValue(1, Duplex.Read)
        assertThat(mapFlow.getValue(Duplex.Read)).isEqualTo(1)

        mapFlow.setValue(2, Duplex.Read)
        mapFlow.setValue(3, Duplex.Read)
        assertThat(mapFlow.getValue(Duplex.Read)).isEqualTo(3)
    }

    @Test
    fun `test WRITE duplex`() {
        val mapFlow = MutableMapFlow(0)
        assertThat(mapFlow.getValue(Duplex.Write)).isNotNull()
        assertThat(mapFlow.getValue(Duplex.Write)).isEqualTo(0)

        mapFlow.setValue(1, Duplex.Write)
        assertThat(mapFlow.getValue(Duplex.Write)).isEqualTo(1)

        mapFlow.setValue(2, Duplex.Write)
        mapFlow.setValue(3, Duplex.Write)
        assertThat(mapFlow.getValue(Duplex.Write)).isEqualTo(3)
    }

    @Test
    fun `test collect on READ duplex`() = runBlocking {
        val mapFlow = MutableMapFlow(0)
        val duplex = Duplex.Read
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            mapFlow.collect(duplex) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            mapFlow.setValue(i, duplex)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }

    @Test
    fun `test collect on WRITE duplex`() = runBlocking {
        val mapFlow = MutableMapFlow(0)
        val duplex = Duplex.Write
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            mapFlow.collect(duplex) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            mapFlow.setValue(i, duplex)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }
}
