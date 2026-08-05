/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.toolkit.featureforms.internal.components.attachment.deleteIfExists
import com.arcgismaps.toolkit.featureforms.internal.utils.runCatchingCancellable
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.junit.Assert.assertThrows
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tests for the various utility functions related to attachment files in the Feature Forms toolkit.
 *
 * @since 300.1.0
 */
class AttachmentFileUtilityTests {

    /**
     * Given a coroutine that throws a [CancellationException],
     * When the [runCatchingCancellable] function is invoked,
     * Then the [CancellationException] should be rethrown and not caught by the function
     */
    @Test
    fun `runCatchingCancellable rethrows cancellation`() {
        assertThrows(CancellationException::class.java) {
            runCatchingCancellable<Unit> {
                throw CancellationException("Cancelled")
            }
        }
    }

    /**
     * Given an existing temporary file,
     * When the file is deleted using the [deleteIfExists] extension function,
     * Then the file should be deleted successfully and no exception should be thrown.
     *
     * @since 300.1.0
     */
    @Test
    fun `deletes existing file`() = runTest {
        val file = File.createTempFile("attachment-test", ".tmp")
        file.deleteIfExists()
        assertThat(file.exists()).isFalse()
    }

    /**
     * Given a non-existing temporary file,
     * When the [deleteIfExists] extension function is called on it,
     * Then no exception should be thrown and the function should complete successfully.
     *
     * @since 300.1.0
     */
    @Test
    fun `does not throw when deleting non-existing file`() = runTest {
        val file = File.createTempFile("attachment-test", ".tmp")
        assertThat(file.delete()).isTrue() // Ensure the file is deleted
        file.deleteIfExists() // Should not throw
        assertThat(file.exists()).isFalse()
    }

    /**
     * Given a coroutine that is performing some operation on a temporary file,
     * When the coroutine is canceled,
     * Then the temporary file is deleted during cancellation cleanup.
     *
     * @since 300.1.0
     */
    @Test
    fun `deletes file when calling coroutine is cancelled`() = runTest {
        val file = File.createTempFile("attachment-test", ".tmp")
        val deferred = CompletableDeferred<Unit>()

        val job = launch {
            try {
                deferred.complete(Unit)
                awaitCancellation()
            } finally {
                file.deleteIfExists()
            }
        }

        // Wait until the coroutine has started and is awaiting cancellation
        deferred.await()
        // Cancel the coroutine
        job.cancelAndJoin()

        assertThat(job.isCancelled).isTrue()
        // Check that the file has been deleted
        assertThat(file.exists()).isFalse()
    }
}
