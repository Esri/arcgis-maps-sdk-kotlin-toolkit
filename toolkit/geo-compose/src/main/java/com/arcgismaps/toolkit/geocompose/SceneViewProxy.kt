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
    /**
     * The view-based [com.arcgismaps.mapping.view.SceneView] that this SceneViewProxy will operate on. This should
     * be initialized by the composable [SceneView] when it enters the composition and set to null when
     * it is disposed by calling [setSceneView].
     *
     * @since 200.4.0
     */
    private var sceneView: com.arcgismaps.mapping.view.SceneView? = null
        set(value) {
            setGeoView(value)
        }

    /**
     * Sets the [sceneView] parameter on this operator. This should be called by the composable [SceneView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.4.0
     */
    internal fun setSceneView(sceneView: com.arcgismaps.mapping.view.SceneView?) {
        this.sceneView = sceneView
    }
}