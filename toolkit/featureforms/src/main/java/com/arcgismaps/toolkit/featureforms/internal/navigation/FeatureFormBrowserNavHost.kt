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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormBrowser
import com.arcgismaps.toolkit.featureforms.FeatureFormBrowserState
import com.arcgismaps.toolkit.featureforms.internal.editor.BrowserOverview
import com.arcgismaps.toolkit.featureforms.internal.editor.FeatureFormNavigationBar
import com.arcgismaps.toolkit.featureforms.rememberNavController

internal sealed class FeatureFormBrowserNavigationRoute(val route: String) {
    object Browser : FeatureFormBrowserNavigationRoute("browser")
    object Overview : FeatureFormBrowserNavigationRoute("overview")
}

@Composable
internal fun FeatureFormBrowserNavHost(
    navController: NavHostController,
    state: FeatureFormBrowserState,
    modifier: Modifier = Modifier,
    showNavigationBar: Boolean = true,
    onShowOnMapRequest: (ArcGISFeature) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = FeatureFormBrowserNavigationRoute.Browser,
        modifier = modifier
    ) {
        composable<FeatureFormBrowserNavigationRoute.Browser> {
            val featureFormState by rememberUpdatedState(state.featureFormState)
            Column(modifier = modifier) {
                FeatureForm(
                    featureFormState = featureFormState,
                    navController = rememberNavController(featureFormState),
                    modifier = Modifier.weight(1f),
                    showFormActions = false,
                    allowNavigationWithEdits = true,
                    onShowOnMapRequest = onShowOnMapRequest,
                    onDismiss = onDismiss
                )
                if (showNavigationBar) {
                    FeatureFormNavigationBar(
                        state = state,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        composable<FeatureFormBrowserNavigationRoute.Overview> {
            BrowserOverview(
                forms = state.featureForms.collectAsState().value
            )
        }
    }
}
