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

package com.arcgismaps.toolkit.utilitynetworks

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.Guid
import com.arcgismaps.utilitynetworks.UtilityElementTraceResult
import com.arcgismaps.utilitynetworks.UtilityNamedTraceConfiguration
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TraceToolTests : UtilityNetworksTestRunner(
    url = "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
    itemId = "471eb0bf37074b1fbb972b1da70fb310"
) {
    private lateinit var context: Context
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setContent() = runTest {
        composeTestRule.setContent {
            val traceState: TraceState = object : TraceState {
                private val _traceConfigurations = MutableStateFlow(null)
                override val traceConfigurations: StateFlow<List<UtilityNamedTraceConfiguration>?> =
                    _traceConfigurations.asStateFlow()

                private val _traceResult = MutableStateFlow<UtilityElementTraceResult?>(null)
                override val traceResult: StateFlow<UtilityElementTraceResult?> =
                    _traceResult.asStateFlow()

                override suspend fun trace() {
                    val utilityNetworkDefinition = utilityNetwork.definition
                    val utilityNetworkSource =
                        utilityNetworkDefinition?.getNetworkSource("Electric Distribution Line")
                    val utilityAssetGroup = utilityNetworkSource?.getAssetGroup("Medium Voltage")
                    val utilityAssetType =
                        utilityAssetGroup?.getAssetType("Underground Three Phase")
                    val startingLocation = utilityNetwork.createElementOrNull(
                        utilityAssetType!!,
                        Guid("0B1F4188-79FD-4DED-87C9-9E3C3F13BA77")
                    )

                    val utilityTraceParameters = UtilityTraceParameters(
                        UtilityTraceType.Connected,
                        listOf(startingLocation!!)
                    )

                    utilityNetwork.trace(
                        utilityTraceParameters
                    ).onSuccess {
                        // Handle trace results
                        _traceResult.value = it[0] as UtilityElementTraceResult
                        Log.i("UtilityNetworkTraceApp", "Trace results: $it")
                        Log.i(
                            "UtilityNetworkTraceApp",
                            "Trace result element size: ${(_traceResult.value)?.elements?.size}"
                        )
                    }.onFailure {
                        // Handle error
                    }
                }
            }

            Trace(
                traceState = traceState
            )
        }
    }

    /**
     * Given a Trace composable
     * When it is rendered
     * Then the top level Surface exists
     *
     * @since 200.6.0
     */
    @Test
    fun testTraceToolSurface() {
        val surface = composeTestRule.onNodeWithContentDescription(traceSurfaceContentDescription)
        surface.assertExists("the base surface of the Trace tool composable does not exist")
    }
}
