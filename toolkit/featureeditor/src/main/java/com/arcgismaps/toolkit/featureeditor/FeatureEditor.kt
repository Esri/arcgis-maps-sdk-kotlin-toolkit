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

package com.arcgismaps.toolkit.featureeditor

import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class FeatureEditor(private val geometryEditor: GeometryEditor) {

    public var featureForm: FeatureForm? = null
        private set

    private val _isStarted = MutableStateFlow(false)
    public val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    public fun start(feature: ArcGISFeature) {
        if (isStarted.value) return

        assert(!geometryEditor.isStarted.value) {
            "Geometry editor should not be started when feature editor is not started."
        }

        val table = feature.featureTable
        // TODO: handle this case by not showing a form
        require(table != null) { "The feature to be edited must have an associated table." }
        val formDefinition = (table.layer as FeatureLayer).featureFormDefinition
        // TODO: again, just don't show the form
        require(formDefinition != null) { "The feature to be edited must have a form definition." }

        featureForm = FeatureForm(feature, formDefinition)

        val geometry = feature.geometry
        if (geometry?.let { !it.isEmpty } == true) geometryEditor.start(geometry)
        else geometryEditor.start(table.geometryType) // TODO: all types supported?

        _isStarted.value = true
    }
}
