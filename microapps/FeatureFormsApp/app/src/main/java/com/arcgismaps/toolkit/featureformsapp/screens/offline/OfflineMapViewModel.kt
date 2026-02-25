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
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.ApplicationScope
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltViewModel
class OfflineMapViewModel @Inject constructor(
    portalItemRepository: PortalItemRepository,
    application: Application,
    @ApplicationScope private val scope: CoroutineScope
) : MapViewModel(
    portalItemRepository,
    application,
    scope
) {
    override val map by lazy {
        portalItemRepository.activeOfflineMap!!
    }
}
