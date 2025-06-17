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

package com.arcgismaps.toolkit.mapviewgeometryeditorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.ProgrammaticReticleTool
import com.arcgismaps.toolkit.mapviewgeometryeditorapp.screens.MainScreen
import com.esri.microappslib.theme.MicroAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {
            MicroAppTheme {
                MapViewGeometryEditorApp()
            }
        }
    }
}

@Composable
fun MapViewGeometryEditorApp() {
    // the geometry editor used to manage the editing session
    val geometryEditor = remember { GeometryEditor() }
    MainScreen(geometryEditor)
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MicroAppTheme {
        MapViewGeometryEditorApp()
    }
}
