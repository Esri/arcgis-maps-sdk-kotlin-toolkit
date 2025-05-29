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

package com.arcgismaps.toolkit.ar

import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy

public class FlyoverSceneViewProxy internal constructor(internal val sceneViewProxy: SceneViewProxy) {
    public constructor() : this(SceneViewProxy())

    init {
        sceneViewProxy.setManualRenderingEnabled(true)
    }
}