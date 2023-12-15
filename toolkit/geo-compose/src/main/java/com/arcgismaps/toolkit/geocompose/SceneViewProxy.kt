package com.arcgismaps.toolkit.geocompose

/**
 * Used to perform operations on a composable [SceneView].
 *
 * There should be a one-to-one relationship between a SceneViewProxy and a composable [SceneView]. This
 * relationship is established by passing an instance of SceneViewProxy to the composable [SceneView] function.
 * Operations can only be performed once the associated composable SceneView has entered the composition.
 * Operations performed when the associated composable SceneView is not in the composition will fail gracefully,
 * i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.4.0
 */
public class SceneViewProxy : GeoViewProxy("SceneView") {

}