/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.overviewmapapp

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.Viewpoint

/**
 * The view model.
 *
 * @param application the application associated with this view model
 * @constructor constructs a viewmodel
 *
 * @since 200.8.0
 */
class ViewModel(application: Application) : AndroidViewModel(application) {
    private val initialViewpoint = Viewpoint(latitude = 39.8, longitude = -98.6, scale = 10e7)

    var viewpointForMapView by mutableStateOf(initialViewpoint)

    var viewpointForSceneView by mutableStateOf(initialViewpoint)
}
