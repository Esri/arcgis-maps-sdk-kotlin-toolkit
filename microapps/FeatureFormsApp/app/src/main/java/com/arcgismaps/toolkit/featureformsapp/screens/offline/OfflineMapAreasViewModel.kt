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

package com.arcgismaps.toolkit.featureformsapp.screens.offline

import android.app.Application
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.ApplicationScope
import com.arcgismaps.toolkit.featureformsapp.screens.map.BaseMapViewModel
import com.arcgismaps.toolkit.offline.OfflineMapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltViewModel
class OfflineMapAreasViewModel @Inject constructor(
    portalItemRepository: PortalItemRepository,
    application: Application,
    @ApplicationScope private val scope: CoroutineScope
) : BaseMapViewModel(application) {

    private var onOfflineMapSelected: (ArcGISMap?) -> Unit = {}

    val map by lazy {
        ArcGISMap(portalItemRepository.activePortalItem!!)
    }

    val state by lazy {
        OfflineMapState(
            arcGISMap = map,
            onSelectionChanged = {
                portalItemRepository.setActiveOfflineMap(it)
                onOfflineMapSelected.invoke(it)
            }
        )
    }

    fun setOnOfflineMapSelectedListener(listener: (ArcGISMap?) -> Unit) {
        onOfflineMapSelected = listener
    }
}
