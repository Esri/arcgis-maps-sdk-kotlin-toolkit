/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureeditorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureeditorapp.screens.FeatureEditorApp
import com.arcgismaps.toolkit.featureeditorapp.screens.FeatureEditorAppState
import com.arcgismaps.toolkit.featureeditorapp.ui.theme.FeatureEditorAppTheme
import kotlinx.coroutines.launch

private const val WEB_MAP_URL =
    "https://runtimecoretest.maps.arcgis.com/home/item.html?id=df0f27f83eee41b0afe4b6216f80b541"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val state = FeatureEditorAppState()

        lifecycleScope.launch {

            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(
                TokenCredential.create(
                    WEB_MAP_URL,
                    BuildConfig.WEB_MAP_USERNAME,
                    BuildConfig.WEB_MAP_PASSWORD
                ).getOrThrow()
            )

            val map = ArcGISMap(WEB_MAP_URL)

            state.setMap(map)
        }

        setContent {
            FeatureEditorAppTheme {
                FeatureEditorApp(state)
            }
        }
    }
}
