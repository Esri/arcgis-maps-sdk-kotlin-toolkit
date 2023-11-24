/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureeditor

import com.arcgismaps.toolkit.featureforms.FeatureFormState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class FeatureEditorState(
    public val featureEditor: FeatureEditor,
    public val featureFormState: FeatureFormState,
    scope: CoroutineScope, // TODO: needed? it's also causing weird initialization problems further up in the app
) {

    private val _isStarted =  MutableStateFlow<Boolean>(featureEditor.isStarted.value)
    public val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            featureEditor.isStarted.collect { // TEST: make sure this works when the editor is already started
                featureEditor.featureForm?.let { featureFormState.setFeatureForm(it) }
                // Propagate this to our own isStarted flow so that we know the feature form state is being updated =
                // before clients of this class see the new value of isStarted.
                _isStarted.value = it
            }
        }
    }
}
