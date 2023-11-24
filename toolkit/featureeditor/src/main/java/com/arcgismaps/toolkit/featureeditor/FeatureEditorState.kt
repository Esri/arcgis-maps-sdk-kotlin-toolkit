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
import kotlinx.coroutines.launch

public class FeatureEditorState(
    public val featureEditor: FeatureEditor,
    public val featureFormState: FeatureFormState,
    scope: CoroutineScope, // TODO: needed? it's also causing weird initialization problems further up in the app
) {
    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            featureEditor.isStarted.collect { // TEST: make sure this works when the editor is already started
                featureEditor.featureForm?.let { featureFormState.setFeatureForm(it) }
            }
        }
    }
}
