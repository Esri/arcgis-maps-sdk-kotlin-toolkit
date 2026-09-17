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

package com.arcgismaps.toolkit.featureforms

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.editor.objectId
import com.arcgismaps.toolkit.featureforms.internal.navigation.FeatureFormBrowserNavigationRoute
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.compose
import kotlinx.coroutines.launch

internal class FeatureFormBrowser(
    features: List<ArcGISFeature>
) {
    val featureForms: StateFlow<List<FeatureForm>>
        field: MutableStateFlow<List<FeatureForm>> = MutableStateFlow(features.map { FeatureForm(it) }.toMutableList())

    fun addFeature(feature: ArcGISFeature) {
        if (featureForms.value.any { it.feature == feature }.not()) {
            featureForms.value = featureForms.value + FeatureForm(feature)
        }
    }

    fun removeFeature(feature: ArcGISFeature) {
        featureForms.value = featureForms.value.filterNot { it.feature == feature }
    }

    suspend fun evaluateExpressions() {
        featureForms.value.forEach { it.evaluateExpressions() }
    }

    suspend fun discardEdits() {
        featureForms.value.forEach { it.discardEdits() }
    }

    suspend fun finishEditing() {
        featureForms.value.forEach { it.finishEditing() }
    }
}

public class FeatureFormBrowserState(
    features: List<ArcGISFeature>,
    private val scope: CoroutineScope
) {
    private val _featureFormBrowser = FeatureFormBrowser(features)

    private var navigateToRoute: ((FeatureFormBrowserNavigationRoute) -> Unit)? = null

    internal val featureFormState = FeatureFormState(
        featureForms.value.first(),
        scope
    )

    public val featureForms: StateFlow<List<FeatureForm>>
        get() = _featureFormBrowser.featureForms

    public val activeFeatureForm: FeatureForm
        get() = featureFormState.activeFeatureForm

    init {
        scope.launch {
            snapshotFlow {
                activeFeatureForm
            }.collect {
                addFeature(it.feature)
            }
        }
    }

    public fun addFeature(feature: ArcGISFeature) {
        _featureFormBrowser.addFeature(feature)
    }

    public fun removeFeature(feature: ArcGISFeature) {
        _featureFormBrowser.removeFeature(feature)
    }

    public suspend fun evaluateExpressions() {
        _featureFormBrowser.evaluateExpressions()
    }

    public suspend fun discardEdits() {
        _featureFormBrowser.discardEdits()
    }

    public suspend fun finishEditing() {
        _featureFormBrowser.finishEditing()
    }

    internal fun setNavigationCallback(navigateToRoute: ((FeatureFormBrowserNavigationRoute) -> Unit)?) {
        this.navigateToRoute = navigateToRoute
    }

    internal fun navigateTo(route: FeatureFormBrowserNavigationRoute) {
        navigateToRoute?.invoke(route)
    }
}
