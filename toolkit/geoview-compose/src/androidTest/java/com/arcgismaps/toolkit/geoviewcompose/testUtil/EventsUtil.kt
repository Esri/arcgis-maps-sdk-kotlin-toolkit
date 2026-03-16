/*
 * COPYRIGHT 1995-2022 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.geoviewcompose.testUtil

import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import java.io.Closeable

/**
 * Test cases on SharedFlows are written as extensions function of this class.
 *
 * The test case on the SharedFlow is itself an [extension on SharedFlow][SharedFlow.test], which
 * takes as a parameter an extension function on this class.
 *
 * @since 200.0.0
 */
class EventsChannel<T>(private val collectionTimeout: Long = 1000) : Closeable {
    val channel = Channel<Result<T>>(UNLIMITED)

    /**
     * Indicates whether the channel currently has no values.
     * This property can be used in tests to assert that the channel is empty.
     * It returns true if the channel is empty, and false otherwise.
     *
     * @since 200.8.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val hasNoValue = channel.isEmpty

    /**
     * Safely send the event result to the channel. If this throws an exception,
     * it means that our timer has expired before an event was emitted but
     * before the test completed.
     *
     * @param message the emitted value from the Event.
     * @since 200.2.0
     */
    @OptIn(DelicateCoroutinesApi::class)
    @ExperimentalCoroutinesApi
    suspend fun trySend(message: Result<T>): Boolean = try {
        @Suppress("OPT_IN_USAGE")
        if (!channel.isClosedForSend) {
            channel.send(message)
            true
        } else {
            false
        }
    } catch (e: Throwable) {
        if (e is CancellationException) {
            throw e
        }
        false
    }

    /**
     * Get the next value emitted from the SharedFlow.
     *
     * The timeout will not delay when calling this from runTest. So it is required that the value
     * already be sent (collected) when this function is called. Alternatively, we could use
     * non-suspending tryReceive() which is a bit different but logically the same. I left the timeout
     * In case we want to use non-test coroutines at some point.
     *
     * @return the emitted value wrapped in a result, or the Throwable that was thrown, if any.
     * @since 200.0.0
     */
    suspend fun awaitValue(): Result<T> =
        try {
            if (collectionTimeout <= 0) {
                channel.receive()
            } else {
                withTimeout(collectionTimeout) {
                    channel.receive()
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }
            Result.failure(t)
        }

    override fun close() {
        channel.close()
    }
}

/**
 * A convenience function to automatically fail a test if the [Result] containing the emitted value
 * is of type [Result.Failure].
 *
 * @since 200.0.0
 */
suspend fun <T> EventsChannel<T>.awaitOrFail() =
    awaitValue().onFailure { fail(it.message) }

/**
 * Suspends until an event occurs on this EventsChannel.
 * If the suspending operation fails, an exception is thrown with the message of the associated
 * Throwable. Otherwise, the specified block is executed.
 */
suspend fun <T> EventsChannel<T>.awaitEvent(block: (T) -> Unit): Result<T> =
    awaitValue()
        .onFailure { fail(it.message) }
        .onSuccess { block(it) }

/**
 * This function provides the ability to write test cases on [Flow]. Emitted values
 * are buffered in the order in which they were emitted, and are retrieved by calling
 * [EventsChannel.awaitValue].
 *
 * @param failIfUnhandledValues if true, will assert that the underlying [EventsChannel.channel] has no more events
 * once [testBlock] completes.
 * @param collectionTimeout maximum time in milliseconds to wait for the next value to be emitted.
 * @param collectionDispatcher the CoroutineDispatcher to use for launching a collecting coroutine that
 * collects from the [Flow] specified as the receiver of this function.
 * @param testBlock an extension function on [EventsChannel], passed as a trailing lambda.
 * @since 200.0.0
 */
@ExperimentalCoroutinesApi
suspend fun <T> Flow<T>.test(
    failIfUnhandledValues: Boolean = true,
    collectionTimeout: Long = 1_000,
    collectionDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    testBlock: suspend EventsChannel<T>.() -> Unit
) {
    val eventsChannel = EventsChannel<T>(collectionTimeout)
    eventsChannel.use {
        // Using an unconfined test dispatcher will run synchronously right up until the first suspend
        // call, then defer to the test scheduler. See the doc on UnconfinedTestDispatcher for an
        // almost identical use case with StateFlows.
        coroutineScope {
            val job = launch(context = collectionDispatcher, start = CoroutineStart.UNDISPATCHED) {
                try {
                    this@test.collect {
                        eventsChannel.trySend(Result.success(it))
                    }
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        eventsChannel.trySend(Result.failure(t))
                    }
                }
            }
            // testBlock is executed on the dispatcher of the calling coroutine, i.e. typically this
            // would be the StandardTestDispatcher if this function is called from runTest.
            eventsChannel.testBlock()
            job.cancelAndJoin()
        }
        if (failIfUnhandledValues) {
            assertWithMessage("There are unhandled events in the event channel after the test has ended.")
                .that(eventsChannel.channel.isEmpty)
                .isTrue()
        }
    }
}

/**
 * Creates a coroutine to collect this [Flow] and returns its future result as a [Deferred]. The result will contain the
 * first value emitted by this [Flow] which satisfies the [predicate]. The coroutine will be launched on the provided [coroutineScope].
 *
 * @param coroutineScope The scope on which to launch the new coroutine
 * @param predicate A function to determine whether the element should be collected and returned
 * @see CoroutineScope.async
 * @see Flow.first
 * @since 200.1.0
 */
suspend fun <T> Flow<T>.firstAsync(
    coroutineScope: CoroutineScope,
    predicate: suspend (T) -> Boolean = { true }
): Deferred<T> =
    coroutineScope.async(start = CoroutineStart.UNDISPATCHED) {
        this@firstAsync.first(predicate)
    }
