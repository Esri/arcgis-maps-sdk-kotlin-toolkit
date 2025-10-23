/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.authentication.utils

import kotlinx.coroutines.CancellableContinuation
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext

/**
 * A continuation that can be safely resumed only once.
 *
 * This is useful for callback-style APIs that may invoke completion more than once.
 *
 * @since 200.8.1
 */
internal class SafeContinuation<T> {
    /**
     *  The underlying cancellable continuation.
     *  It is initialized when [suspendCancellable] is called, which should be set before
     *  [resumeSafely] or [invokeOnCancellation] is called.
     */
    private lateinit var continuation: CancellableContinuation<T>

    /**
     * Indicates whether this continuation has already been resumed.
     * This is used to ensure that [resumeSafely] only resumes the continuation once.
     */
    private val hasResumed: AtomicBoolean = AtomicBoolean(false)

    /**
     * Suspends using [suspendCancellableCoroutine] and exposes this [SafeContinuation] for callback-style
     * APIs that may invoke completion more than once. Guarantees only the first resume will be executed.
     *
     * @param block Invoked immediately with this [SafeContinuation] to hook into external callbacks.
     * @return the successful value.
     * @since 200.8.1
     */
    suspend inline fun suspendCancellable(
        crossinline block: (SafeContinuation<T>) -> Unit
    ): T = suspendCancellableCoroutine {
        continuation = it
        block(this)
    }

    /**
     * Resumes this continuation with the given [value] if it has not already been resumed.
     *
     * @param value The value to resume the continuation with.
     * @param onCancellation Optional lambda to be invoked if the continuation is cancelled after resuming.
     * @since 200.8.1
     */
    fun <R : T> resumeSafely(
        value: R,
        onCancellation: ((cause: Throwable, value: R, context: CoroutineContext) -> Unit) = { _, _, _ -> }
    ) {
        if (hasResumed.compareAndSet(false, true)) {
            continuation.resume(value, onCancellation)
        }
    }


    /**
     * Registers a handler to be invoked if the continuation is cancelled.
     *
     * @param block The block to invoke on cancellation.
     * @since 200.8.1
     */
    fun invokeOnCancellation(block: () -> Unit) {
        continuation.invokeOnCancellation { block() }
    }
}

/**
 * Suspends using [suspendCancellableCoroutine] and exposes a [SafeContinuation] for callback-style
 * APIs that may invoke completion more than once. Guarantees only the first resume will be executed.
 * Use [SafeContinuation.resumeSafely] to resume the continuation or [SafeContinuation.invokeOnCancellation]
 * to handle cancellation.
 *
 * @param block Invoked immediately with a [SafeContinuation] to hook into external callbacks.
 * @return the successful value.
 * @since 200.8.1
 */
internal suspend inline fun <T> safeSuspendableCancellable(
    crossinline block: (SafeContinuation<T>) -> Unit
): T = SafeContinuation<T>().suspendCancellable {
    block(it)
}
