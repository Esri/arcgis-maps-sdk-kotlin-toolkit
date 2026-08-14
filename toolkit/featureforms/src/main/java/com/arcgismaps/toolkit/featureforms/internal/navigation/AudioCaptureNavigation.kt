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

package com.arcgismaps.toolkit.featureforms.internal.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentSource
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AudioCapture
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AudioCaptureViewModel
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.addAttachmentFromFile
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.audioCaptureDestination(
    onDismissRequest: (NavBackStackEntry) -> Unit,
    state: FeatureFormState,
) {
    dialog<NavigationRoute.AudioCapture>(
        dialogProperties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) { backStackEntry ->
        val scope = rememberCoroutineScope()
        val route = backStackEntry.toRoute<NavigationRoute.AudioCapture>()
        val formData = state.getActiveFormStateData()
        val states = formData.stateCollection
        val attachmentElementState = states[route.stateId] as? AttachmentElementState
        if (attachmentElementState != null) {
            val viewModel = viewModel<AudioCaptureViewModel>(
                factory = AudioCaptureViewModel.Factory(
                    maxDuration = route.maxDuration ?: Long.MAX_VALUE,
                    onAudioCaptured = { file ->
                        attachmentElementState.addAttachmentFromFile(
                            file = file,
                            source = AttachmentSource.Capture
                        )
                    }
                )
            )
            AudioCapture(
                viewModel = viewModel,
                maxDuration = null,
                onDismissRequest = {
                    onDismissRequest(backStackEntry)
                },
                onAudioCaptured = {}
            )
        }
    }
}

internal fun NavHostController.navigateToAudioCapture(
    backStackEntry: NavBackStackEntry,
    maxDuration: Long?,
    stateId: Int,
) {
    val newRoute = NavigationRoute.AudioCapture(maxDuration, stateId)
    navigateSafely(backStackEntry, newRoute)
}
