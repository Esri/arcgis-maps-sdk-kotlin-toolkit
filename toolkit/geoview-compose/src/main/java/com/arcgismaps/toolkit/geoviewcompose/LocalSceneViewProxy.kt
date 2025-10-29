/*
 COPYRIGHT 1995-2025 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.view.LocalSceneView

/**
 * Used to perform operations on a composable [LocalSceneView].
 *
 * There should be a one-to-one relationship between a LocalSceneViewProxy and a composable
 * [LocalSceneView]. This relationship is established by passing an instance of LocalSceneViewProxy
 * to the composable [LocalSceneView] function. Operations can only be performed once the associated
 * composable LcoalSceneView has entered the composition. Operations performed when the associated
 * composable LocalSceneView is not in the composition will fail gracefully, i.e. won't throw
 * exceptions but won't return a successful result.
 *
 * @since 300.0.0
 */
@Stable
public class LocalSceneViewProxy : GeoViewProxy("LocalSceneView") {
    /**
     * The view-based [com.arcgismaps.mapping.view.LocalSceneView] that this LocalSceneViewProxy
     * will operate on. This should be initialized by the composable [LocalSceneView] when it enters
     * the composition and set to null when it is disposed by calling [setLocalSceneView].
     *
     * @since 300.0.0
     */
    private var localSceneView: LocalSceneView? = null
        set(value) {
            setGeoView(value)
            field = value
        }

    /**
     * Sets the [localSceneView] parameter on this operator. This should be called by the composable
     * [LocalSceneView] when it enters the composition and set to null when it is disposed.
     *
     * @since 300.0.0
     */
    internal fun setLocalSceneView(localSceneView: LocalSceneView?) {
        this.localSceneView = localSceneView
    }
}