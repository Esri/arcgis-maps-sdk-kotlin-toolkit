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
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
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
     * The preferred target element for accessibility focus when content is displayed.
     */
    internal val preferredTargetFocusRequester = FocusRequester()

    /**
     * The main boolean condition to determine if the GeoView (not trailing content)
     * should be focusable and interact with screen readers.
     */
    private val _isGeoViewFocusable = mutableStateOf(canFocus && !isContentBeingDisplayed.get())
    internal val isGeoViewFocusable: State<Boolean>
        get() = _isGeoViewFocusable

    /**
     * Used by GeoViewScope to notify here that trailing content has entered the composition.
     * Update the GeoView to not be focusable and, on [View.post] of the GeoView, request focus
     * into the trailing content.
     */
    internal fun onContentComposed() {
        if (isContentBeingDisplayed.compareAndSet(false, true)) {
            // Continue if user provided property is true
            if (!canFocus) return
            // Set focusable state to propagate an `update` to the AndroidView.
            _isGeoViewFocusable.value = false
            // Clear accessibility focus from GeoView to send it to the content lambda.
            geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null)
            geoView.post {
                // Attempt to focus preferred target in content lambda,
                // if a target is provided by user then boolean should be true.
                val isPreferredTargetFocused = runCatching {
                    preferredTargetFocusRequester.requestFocus()
                }.getOrDefault(false)
                // If preferred target focus request failed, attempt to focus the content as a whole.
                if (!isPreferredTargetFocused){
                    contentFocusRequester.requestFocus()
                }
            }
        }
    }

    /**
     * Used by GeoViewScope to notify here that trailing content has left the composition.
     * Update the GeoView to be focusable again and, on [View.post] of the GeoView, hand focus
     * back it.
     */
    internal fun onContentDisposed() {
        isContentBeingDisplayed.set(false)
        // Continue if user provided property is true
        if (!canFocus) return
        // Set focusable state to propagate an `update` to the AndroidView.
        _isGeoViewFocusable.value = true
        // After AndroidView post, send accessibility focus from content lambda back to GeoView.
        geoView.post {
            // Update isFocusable attribute in case if post was called before mutable state triggered an `update`.
            geoView.isFocusable = true
            geoView.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)
        }
    }
}
