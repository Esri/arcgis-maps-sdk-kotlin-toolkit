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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormNavigationBar
import com.arcgismaps.toolkit.featureforms.internal.navigation.FeatureFormBrowserNavHost

@Composable
public fun FeatureFormBrowser(
    state: FeatureFormBrowserState,
    modifier: Modifier = Modifier,
    showNavigationBar: Boolean = true,
    onDismiss: () -> Unit = {},
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
) {
    val featureFormState by rememberUpdatedState(state.featureFormState)
    val navController = rememberNavController(featureFormState)
    FeatureFormBrowserNavHost(
        navController = navController,
        state = state,
        modifier = modifier,
        showNavigationBar = showNavigationBar,
        onShowOnMapRequest = onShowOnMapRequest,
        onDismiss = onDismiss
    )
    DisposableEffect(navController) {
        state.setNavigationCallback { route ->
            navController.navigate(route)
        }
        onDispose {
            state.setNavigationCallback(null)
        }
    }
}
