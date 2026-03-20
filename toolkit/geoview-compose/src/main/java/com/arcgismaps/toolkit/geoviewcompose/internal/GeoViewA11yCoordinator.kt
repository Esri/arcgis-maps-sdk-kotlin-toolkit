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
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Stable
import androidx.compose.ui.focus.FocusRequester
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Internal coordinator class to manage accessibility focus and configuration for a
 * GeoView and mutually exclusive focus with its trailing lambda content.
 *
 * Track whether the GeoView can be focused and whether content is currently being displayed.
 * Either a GeoView or its content (like Callout) can be focused at a given time, but not both.
 *
 * @param geoView The GeoView whose accessibility configuration is being managed.
 * @param canFocus User provided property to set underlying focus behavior.
 * @param contentFocusRequester FocusRequester used by trailing lambda content to request focus.
 *
 * @since 300.0.0
 */
@Stable
internal class GeoViewA11yCoordinator(
    internal val geoView: View,
    internal val canFocus: Boolean,
    internal val contentFocusRequester: FocusRequester
) {

    init {
        // Set focus right away so initial compose AndroidView "focus interop" is set up correctly.
        geoView.isFocusable = canFocus
    }

    /**
     * Tracks whether content from the related GeoViewScope is currently being displayed.
     * Used to drive focus away from GeoView and into content when displayed.
     */
    private val isContentBeingDisplayed = AtomicBoolean(false)

    /**
     * The main boolean condition to determine if the GeoView (not trailing content)
     * should be focusable and interact with screen readers.
     */
    val isGeoViewFocusable: Boolean
        get() = canFocus && !isContentBeingDisplayed.get()

    /**
     * Used by GeoViewScope to notify here that trailing content has entered the composition.
     * Update the GeoView to not be focusable and, on [View.post] of the GeoView, request focus
     * into the trailing content.
     */
    internal fun onContentComposed() {
        geoView.post {
            if (isContentBeingDisplayed.compareAndSet(false, true)) {
                if (!canFocus) return@post
                geoView.isFocusable = false
                geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, null)
                contentFocusRequester.requestFocus()
            }
        }
    }

    /**
     * Used by GeoViewScope to notify here that trailing content has left the composition.
     * Update the GeoView to be focusable again and, on [View.post] of the GeoView, hand focus
     * back it.
     */
    internal fun onContentDisposed() {
        geoView.post {
            isContentBeingDisplayed.set(false)
            if (!canFocus) return@post
            geoView.isFocusable = true
            geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)
        }
    }
}
