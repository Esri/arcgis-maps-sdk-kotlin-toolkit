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

package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.Loadable
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.ServerTrust
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before

/**
 * A test runner for feature form tests. This class is responsible for loading the map with the
 * given [uri] and the feature with the given [objectId] and initializing the [FeatureForm] for
 * the feature.
 *
 * If the map fails to load or the feature is not found, the test will fail.
 *
 * @param uri The URI of the web map.
 * @param objectId The ID of the feature.
 * @param user The username for authentication. Defaults to [BuildConfig.webMapUser].
 * @param password The password for authentication. Defaults to [BuildConfig.webMapPassword].
 * @param layerName The name of the feature layer. Defaults to an empty string, which means the first
 * layer will be used.
 */
open class FeatureFormTestRunner(
    private val uri: String,
    private val objectId: Long,
    private val user: String = BuildConfig.webMapUser,
    private val password: String = BuildConfig.webMapPassword,
    private val layerName: String = ""
) {
    /**
     * The feature form for the feature with the given [objectId].
     */
    lateinit var featureForm: FeatureForm
        private set

    @Before
    fun setup(): Unit = runTest {
        // If the feature form is already initialized, return
        if (::featureForm.isInitialized) return@runTest
        // Set the authentication challenge handler
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            FeatureFormsTestChallengeHandler(
                user, password
            )
        ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler =
            NetworkAuthenticationChallengeHandler {
                NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)
            }
        // Load the map
        val map = ArcGISMap(uri = uri)
        map.assertIsLoaded()
        map.utilityNetworks.forEach {
            it.assertIsLoaded()
        }
        // Load the feature layer
        val featureLayer = (map.operationalLayers.firstOrNull {
            it.name == layerName
        } ?: map.operationalLayers.firstOrNull()) as? FeatureLayer
        assertThat(featureLayer).isNotNull()
        featureLayer!!.assertIsLoaded()
        // Query the feature
        val parameters = QueryParameters().also {
            it.objectIds.add(objectId)
            it.maxFeatures = 1
        }
        val featureQueryResult = featureLayer.featureTable?.queryFeatures(parameters)?.getOrNull()
        assertThat(featureQueryResult).isNotNull()
        val feature = featureQueryResult!!.find {
            it is ArcGISFeature
        } as? ArcGISFeature
        assertThat(feature).isNotNull()
        feature!!.assertIsLoaded()
        // Initialize the feature form
        featureForm = FeatureForm(feature)
        featureForm.evaluateExpressions()
    }
}

suspend fun Loadable.assertIsLoaded() {
    val result = load()
    assertThat(loadStatus.value).isEqualTo(LoadStatus.Loaded)
    assertThat(result.isSuccess).isTrue()
}
