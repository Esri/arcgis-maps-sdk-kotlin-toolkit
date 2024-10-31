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

package com.arcgismaps.toolkit.utilitynetworks

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before

/**
 * A test runner for utility network tests. This class is responsible for loading the map with the
 * given [url] and the first UtilityNetwork
 * If the map fails to load or the UtilityNetwork is not found, the test will fail.
 *
 * @property url The URL of the web map.
 * @property itemId the item id on agol to load as a webmap
 * @since 200.6.0
  */
open class TraceToolTestRunner(
    private val url: String,
    private val itemId: String
) {
    /**
     * The trace tool TraceState object
     */
    private var _traceState: TraceState? = null
    internal val traceState: TraceState
        get() = _traceState ?: throw IllegalStateException("trace state is not initialized")

    private var _map: ArcGISMap = ArcGISMap(PortalItem(Portal.arcGISOnline(connection = Portal.Connection.Anonymous), itemId))
    internal val map: ArcGISMap
        get() = _map

    private var _mapviewProxy: MapViewProxy = MapViewProxy()
    internal val mapviewProxy: MapViewProxy
        get() = _mapviewProxy

    private var _graphicsOverlay: GraphicsOverlay = GraphicsOverlay()
    internal val graphicsOverlay: GraphicsOverlay
        get() = _graphicsOverlay

    @Before
    fun setup(): Unit = runTest {
        // If the utility network is already initialized, return
        if (_traceState != null) return@runTest
        // Set the authentication challenge handler
        val tokenCred =
            TokenCredential.create(
                url,
                username = BuildConfig.traceToolUser,
                password = BuildConfig.traceToolPassword
            ).getOrThrow()

        ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCred)
        _traceState = TraceState(map, graphicsOverlay, mapviewProxy)
    }
}
