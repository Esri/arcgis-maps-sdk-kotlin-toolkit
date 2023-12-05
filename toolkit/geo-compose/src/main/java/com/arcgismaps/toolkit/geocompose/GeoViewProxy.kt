/*
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

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.GeoView

/**
 * Used to perform operations on a composable MapView or SceneView.
 *
 * @since 200.3.0
 */
public sealed class GeoViewProxy {

    protected var geoView: GeoView? = null

    /**
     * Sets the [geoView] parameter on this operator. This should be called by the composable [geoView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.3.0
     */
    internal fun setGeoView(geoView: GeoView?) {
        this.geoView = geoView
    }

    /**
     * True if continuous panning across the international date line is enabled in the GeoView, false otherwise.
     *
     * @since 200.3.0
     */
    public val isWrapAroundEnabled: Boolean?
        get() = geoView?.isWrapAroundEnabled
}
