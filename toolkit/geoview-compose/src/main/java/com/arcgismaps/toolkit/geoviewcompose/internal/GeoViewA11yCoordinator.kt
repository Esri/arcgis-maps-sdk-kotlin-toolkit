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
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    internal val geoView: View
) {
    /**
     * The canFocus property from the related GeoView
     */
    private var canFocus: Boolean = true

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
     * Used by [geoViewA11yDelegate] to update the canFocus state in this coordinator
     * when the related GeoView's canFocus property changes.
     */
    internal fun updateCanFocus(canFocus: Boolean) {
        this.canFocus = canFocus
    }

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
                calloutView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            }
        }
    }

    /**
     * Used by GeoViewScope to notify here that a Callout is being dismissed.
     * Update GeoView to be focusable again and, on [View.post] of the geoView, request focus
     * back to the GeoView and send a focused accessibility event.
     */
    internal fun onCalloutDisposed() {
        isCalloutBeingDisplayed.set(false)
        if (!canFocus) return
        applyGeoViewA11yConfiguration()
        geoView.post {
            geoView.requestFocus()
            geoView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
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
        // Set the GeoView to not be considered a View with live region content
        if (isGeoViewFocusable) {
            geoView.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
        }
        if (geoView is ViewGroup) {
            // Allow parent GeoView to be focused before its descendants for KeyListener control.
            geoView.descendantFocusability = when {
                isGeoViewFocusable -> ViewGroup.FOCUS_BEFORE_DESCENDANTS
                else -> ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        }
        // If GeoView should not be focusable, clear any existing focus.
        if (!isGeoViewFocusable) {
            geoView.clearFocus()
            geoView.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
                null
            )
            geoView.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLEAR_FOCUS,
                null
            )
        }
    }
}

/**
 * Provides [AccessibilityDelegate] for GeoView to mirror the focusability from [GeoViewA11yCoordinator]
 * and to consume child focus/population events when the GeoView is already focused.
 *
 * @param coordinator The [GeoViewA11yCoordinator] that holds the state to determine GeoView's accessibility configuration.
 * @param canFocus The current canFocus state from GeoView to keep the coordinator in sync with the GeoView's state.
 *
 * @return An [AccessibilityDelegate] to be applied to the GeoView for accessibility configuration and event handling.
 * @since 300.0.0
 */
@Composable
internal fun geoViewA11yDelegate(
    coordinator: GeoViewA11yCoordinator,
    canFocus: Boolean,
): AccessibilityDelegate {
    // When canFocus changes, update the coordinator to keep in sync with GeoView's state.
    val latestCanFocus by rememberUpdatedState(canFocus)
    SideEffect {
        coordinator.updateCanFocus(latestCanFocus)
    }
    // Create and remember the AccessibilityDelegate applied to the GeoView.
    // Mirror the focusable state from the coordinator and avoid child focus/population events.
    return remember(coordinator) {
        object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                // Supply the [host] to the coordinator to apply accessibility configuration based on the current state.
                coordinator.applyGeoViewA11yConfiguration(host)
                super.onInitializeAccessibilityNodeInfo(host, info)
                // Mirror the GeoView's focusability to the AccessibilityNodeInfo.
                val enabled = coordinator.isGeoViewFocusable
                info.apply {
                    isFocusable = enabled
                    isFocused = enabled && host.isFocused
                }
            }

            override fun dispatchPopulateAccessibilityEvent(
                host: View,
                event: AccessibilityEvent,
            ): Boolean {
                // Consume child focus event population when the GeoView is already in focus
                return if (
                    host.isFocused &&
                    event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                ) true
                else super.dispatchPopulateAccessibilityEvent(host, event)
            }
        }
    }
}
