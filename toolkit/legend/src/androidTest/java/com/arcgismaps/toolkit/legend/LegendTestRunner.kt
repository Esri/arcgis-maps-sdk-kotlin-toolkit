/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.legend

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.util.concurrent.CountDownLatch

/**
 * A test runner for utility network tests. This class is responsible for loading the map with the
 * given [url] and the first UtilityNetwork
 * If the map fails to load or the UtilityNetwork is not found, the test will fail.
 *
 * @property url The URL of the web map.
 * @property itemId the item id on agol to load as a webmap
 * @since 200.6.0
  */
open class LegendTestRunner(
    private val url: String,
    private val itemId: String
) {
    /**
     * The trace tool TraceState object
     */
    // Create a CountDownLatch to wait for the draw status change
    internal val drawStatusLatch = CountDownLatch(1)

    @Before
    fun setup(): Unit = runTest {
        // If the utility network is already initialized, return
//        if (_traceState != null) return@runTest
    }
}
