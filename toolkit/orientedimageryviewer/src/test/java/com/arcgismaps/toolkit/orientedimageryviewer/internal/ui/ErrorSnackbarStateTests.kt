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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorSnackbarStateTests {
    @Test
    fun `errors are shown with a dismiss action`() = runTest {
        val state = ErrorSnackbarState(backgroundScope)
        state.showError("No active image")
        runCurrent()

        val visuals = requireNotNull(state.hostState.currentSnackbarData).visuals
        assertEquals("No active image", visuals.message)
        assertEquals(true, visuals.withDismissAction)
    }

    @Test
    fun `new errors replace the visible message without queuing stale errors`() = runTest {
        val state = ErrorSnackbarState(backgroundScope)
        state.showError("First error")
        runCurrent()
        state.showError("Second error")
        state.showError("Latest error")
        runCurrent()

        assertEquals("Latest error", state.hostState.currentSnackbarData?.visuals?.message)
        state.hostState.currentSnackbarData?.dismiss()
        runCurrent()
        assertNull(state.hostState.currentSnackbarData)
    }

    @Test
    fun `same error can be shown again after dismissal`() = runTest {
        val state = ErrorSnackbarState(backgroundScope)
        state.showError("No active image")
        runCurrent()
        state.hostState.currentSnackbarData?.dismiss()
        runCurrent()
        state.showError("No active image")
        runCurrent()

        assertEquals("No active image", state.hostState.currentSnackbarData?.visuals?.message)
    }
}

