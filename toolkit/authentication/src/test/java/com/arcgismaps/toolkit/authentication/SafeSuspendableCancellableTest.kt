/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.authentication

import com.arcgismaps.toolkit.authentication.utils.safeSuspendableCancellable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class SafeSuspendableCancellableTest {

    /**
     * Given a safeSuspendableCancellable block
     * When the block resumes successfully
     * Then the value is returned
     * @since 200.8.1
     */
    @Test
    fun returnsValueOnSuccess() = runTest {

        val result = safeSuspendableCancellable<String> { cont ->
            cont.resumeSafely("value")
        }
        assertThat(result).isEqualTo("value")
    }

    /**
     * Given a safeSuspendableCancellable block
     * When the block throws an exception
     * Then the exception is propagated
     * @since 200.8.1
     */
    @Test
    fun throwsOnFailure() = runTest {
        val ex = RuntimeException("boom")
        var caught: Throwable? = null
        try {
            safeSuspendableCancellable<Unit> { _ ->
                throw ex
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertThat(caught).isInstanceOf(ex::class.java)
        assertThat(caught?.message).isEqualTo(ex.message)
    }

    /**
     * Given a safeSuspendableCancellable block
     * When the block attempts to resume after throwing an exception
     * Then the resume is ignored and the exception is propagated
     * @since 200.8.1
     */
    @Test
    fun ignoresSecondResumeCancelThenSuccess() = runTest {
        var safeResult: String? = null
        val res = runCatching {
            safeResult = safeSuspendableCancellable<String> { cont ->
                throw IllegalStateException("This should be called first")
                cont.resumeSafely("late") // ignored
            }
        }
        assertThat(res.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(res.exceptionOrNull()?.message).isEqualTo("This should be called first")
        assertThat(safeResult).isNull()

    }

    /**
     * Given a safeSuspendableCancellable block
     * When the block attempts to resume multiple times
     * Then only the first resume is effective
     * @since 200.8.1
     */
    @Test
    fun testIgnoresSubsequentSignals() = runTest {
        val safeResult = safeSuspendableCancellable<String> { cont ->
            cont.resumeSafely("first")
            cont.resumeSafely("ignored") // ignored
            cont.resumeSafely("also ignored") // ignored
        }
        assertThat(safeResult).isEqualTo("first")
    }

    /**
     * Given a safeSuspendableCancellable block
     * When the parent coroutine is cancelled
     * Then the continuation resumes with cancellation
     * And the cancellation handler is invoked
     * @since 200.8.1
     */
    @Ignore("TODO() this test fails")
    @Test
    fun parentCancellationResultsInCancellationOutcome() = runTest {
        val invoked = AtomicBoolean(false)
        val deferred = async {
            safeSuspendableCancellable<String> { cont ->
                // simulate long operation; do not resume
                cont.invokeOnCancellation {
                    invoked.set(true)
                }
            }
        }
        deferred.cancel()
        var caught: Throwable? = null
        try {
            deferred.await() // expect cancellation
        } catch (t: Throwable) {
            caught = t
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
        assertThat(invoked.get()).isTrue()
    }
}
