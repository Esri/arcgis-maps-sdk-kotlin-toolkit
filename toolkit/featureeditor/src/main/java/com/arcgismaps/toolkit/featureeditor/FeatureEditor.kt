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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

public sealed class FinishState(public open val feature: ArcGISFeature) {
    public data class Discarded(override val feature: ArcGISFeature) : FinishState(feature)
    public data class Stopped(override val feature: ArcGISFeature) : FinishState(feature)
}

public class FeatureEditor(private val geometryEditor: GeometryEditor) {

    public var featureForm: FeatureForm? = null
        private set

    // IDEA: would be nice to collect these into a session object

    private var currentFeature: ArcGISFeature? = null

    private val _isStarted = MutableStateFlow(false)
    // TODO: can this be defined in terms of whether currentFeature is null?
    public val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val _onFinish = MutableSharedFlow<FinishState>(
        replay = 0, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    public val onFinish: SharedFlow<FinishState> = _onFinish.asSharedFlow()

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

        currentFeature = feature
        featureForm = FeatureForm(feature, formDefinition)

        val geometry = feature.geometry
        if (geometry?.let { !it.isEmpty } == true) geometryEditor.start(geometry)
        else geometryEditor.start(table.geometryType) // TODO: all types supported?

        _isStarted.value = true
    }

    public fun stop() {
        if (!isStarted.value) return

        assert(geometryEditor.isStarted.value) {
            "Geometry editor should be started whenever feature editor is started."
        }

        val feature = currentFeature
        assert(currentFeature != null) {
            "Current feature should not be null whenever feature editor is started."
        }

        _onFinish.tryEmit(FinishState.Stopped(feature!!))

        resetState()
    }

    public fun discard() {
        if (!isStarted.value) return

        assert(geometryEditor.isStarted.value) {
            "Geometry editor should be started whenever feature editor is started."
        }

        val feature = currentFeature
        assert(currentFeature != null) {
            "Current feature should not be null whenever feature editor is started."
        }

        /*
            We need separate stop and discard methods so that the feature editor knows whether it was the user's
            intention to save the edits they made or not. It then needs to propagate this information to the app
            developer so they can act accordingly. We have said that the feature editor won't do anything special to
            save edits made to the feature and that it should be the app developer's responsibility instead. But the
            app developer doesn't know which buttons are being pressed on the feature editor so they can't act
            accordingly unless we tell them whether they are supposed to save the feature or not.

            But it seemed weird to have stop and discard methods that do exactly the same thing. Refreshing the feature
            seems like a light way to discard the edits without dabbling with the feature's table.
         */
        feature!!.refresh()

        _onFinish.tryEmit(FinishState.Discarded(feature))
        resetState()
    }

    private fun resetState() {
        featureForm = null
        // It's probably not necessary to push this into the feature again because it should be pushed into the feature
        // regularly in response to the geometry changed event on the geometry editor.
        // TODO: ^ need to actually consume that event here
        val newGeometry = geometryEditor.stop()
        currentFeature?.geometry = newGeometry

        _isStarted.value = false
        currentFeature = null
    }
}
