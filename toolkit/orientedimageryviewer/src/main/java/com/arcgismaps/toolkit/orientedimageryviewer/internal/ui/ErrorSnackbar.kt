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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Presents errors without allowing repeated actions to queue stale messages. */
internal class ErrorSnackbarState(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()
    private var messageJob: Job? = null

    fun showError(message: String) {
        messageJob?.cancel()
        messageJob = scope.launch {
            hostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }
}

/** Hosts reusable error snackbars over content without changing its layout. */
@Composable
internal fun ErrorSnackbarContainer(
    modifier: Modifier = Modifier,
    content: @Composable (ErrorSnackbarState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { ErrorSnackbarState(scope) }
    Box(modifier = modifier) {
        content(state)
        SnackbarHost(
            hostState = state.hostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )
    }
}

