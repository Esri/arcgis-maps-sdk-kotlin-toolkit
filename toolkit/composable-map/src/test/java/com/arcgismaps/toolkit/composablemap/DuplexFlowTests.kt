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
        assertThat(duplexFlow.getValue(Duplex.Read)).isNotNull()
        assertThat(duplexFlow.getValue(Duplex.Read)).isEqualTo(0)

        duplexFlow.setValue(1, Duplex.Read)
        assertThat(duplexFlow.getValue(Duplex.Read)).isEqualTo(1)

        duplexFlow.setValue(2, Duplex.Read)
        duplexFlow.setValue(3, Duplex.Read)
        assertThat(duplexFlow.getValue(Duplex.Read)).isEqualTo(3)
    }

    @Test
    fun `test WRITE duplex`() {
        val duplexFlow = MutableDuplexFlow(0)
        assertThat(duplexFlow.getValue(Duplex.Write)).isNotNull()
        assertThat(duplexFlow.getValue(Duplex.Write)).isEqualTo(0)

        duplexFlow.setValue(1, Duplex.Write)
        assertThat(duplexFlow.getValue(Duplex.Write)).isEqualTo(1)

        duplexFlow.setValue(2, Duplex.Write)
        duplexFlow.setValue(3, Duplex.Write)
        assertThat(duplexFlow.getValue(Duplex.Write)).isEqualTo(3)
    }

    @Test
    fun `test collect on READ duplex`() = runBlocking {
        val duplexFlow = MutableDuplexFlow(0)
        val duplex = Duplex.Read
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            duplexFlow.collect(duplex) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            duplexFlow.setValue(i, duplex)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }

    @Test
    fun `test collect on WRITE duplex`() = runBlocking {
        val duplexFlow = MutableDuplexFlow(0)
        val duplex = Duplex.Write
        val list = mutableListOf<Int>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            duplexFlow.collect(duplex) {
                list.add(it)
            }
        }

        val range = 1..10
        for (i in range) {
            duplexFlow.setValue(i, duplex)
            delay(100)
        }

        assertThat(list).hasSize(range.count() + 1)
        assertThat(list).containsExactlyElementsIn(0..10).inOrder()
        job.cancel()
    }
}
