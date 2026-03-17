/*
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.geoviewcompose.internal

import android.view.View
import android.view.View.AccessibilityDelegate
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Stable
import androidx.compose.ui.focus.FocusRequester
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Internal coordinator class to manage accessibility focus and configuration for a
 * MapView or SceneView or LocalSceneView and its Callout using an [AccessibilityDelegate].
 *
 * Track whether the GeoView can be focused and whether a callout is currently being displayed.
 * Either a GeoView or its Callout can be focused at a given time, but not both.
 *
 * @param calloutFocusRequester FocusRequester for Callout to request focus when displayed.
 * @param geoView The GeoView type whose accessibility configuration is being managed.
 *
 * @since 300.0.0
 */
@Stable
internal class GeoViewA11yCoordinator(
    internal val calloutFocusRequester: FocusRequester,
    internal val canFocus: Boolean,
    internal val geoView: View
) {

    init {
        // Set focus right away so initial compose AndroidView "focus interop" is set up correctly.
        geoView.isFocusable = canFocus
    }

    /**
     * The isCalloutBeingDisplayed property from the related GeoViewScope,
     * Used to drive focus away from GeoView and into Callout when displayed.
     */
    private val isCalloutBeingDisplayed = AtomicBoolean(false)

    /**
     * The main boolean condition to determine if GeoView (not Callout) should be focusable
     * and interact with screen readers.
     */
    val isGeoViewFocusable: Boolean
        get() = canFocus && !isCalloutBeingDisplayed.get()

    /**
     * Used by GeoViewScope to notify here that a Callout is being displayed.
     * Update GeoView to not be focusable and, on [View.post] of the calloutView, request focus
     * into the Callout and send a content changed accessibility event.
     */
    internal fun onCalloutPlaced(calloutView: View) {
        calloutView.post {
            if (isCalloutBeingDisplayed.compareAndSet(false, true)) {
                if (!canFocus) return@post
                applyGeoViewA11yConfiguration()
                calloutFocusRequester.requestFocus()
            }
        }
    }

    /**
     * Used by GeoViewScope to notify here that a Callout is being dismissed.
     * Update GeoView to be focusable again and, on [View.post] of the geoView, request focus
     * back to the GeoView and send a focused accessibility event.
     */
    internal fun onCalloutDisposed() {
        geoView.post {
            isCalloutBeingDisplayed.set(false)
            if (!canFocus) return@post
            applyGeoViewA11yConfiguration()
            geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)
        }
    }

    /**
     * Using the current state of this coordinator, apply accessibility configuration to the GeoView.
     *
     * @param geoView The GeoView to apply configuration, defaults to the constructor property.
     */
    internal fun applyGeoViewA11yConfiguration(geoView: View = this.geoView) {
        // Mirror the focusable property into the GeoView
        geoView.isFocusable = isGeoViewFocusable
        // If GeoView should not be focusable, clear any existing focus.
        if (!isGeoViewFocusable) {
            geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, null)
        }
    }
}
