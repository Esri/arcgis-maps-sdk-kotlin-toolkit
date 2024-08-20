package com.arcgismaps.toolkit.ar

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewScope
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutColors
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutShapes

public class TableTopSceneViewScope internal constructor(private val sceneViewScope: SceneViewScope) {
    /**
     * Displays a Callout at the specified geographical location on the GeoView. The Callout is a composable
     * that can be used to display additional information about a location on the map. The additional information is
     * passed as a content composable that contains text and/or other content. It has a leader that points to
     * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
     * that contains the content lambda provided by the application. A thin border line is drawn around the entire Callout.
     *
     * Note: Only one Callout can be displayed at a time on the GeoView.
     *
     * @param location the geographical location at which to display the Callout
     * @param modifier Modifier to be applied to the composable Callout
     * @param content the content of the Callout
     * @param offset the offset in screen coordinates from the geographical location at which to place the callout
     * @param rotateOffsetWithGeoView specifies whether the screen offset is rotated with the [GeoView]. The Screen offset
     *        will be rotated with the [GeoView] when true, false otherwise.
     *        This is useful if you are showing the callout for elements with symbology that does rotate with the [GeoView]
     * @param colorScheme the styling options for the Callout's color properties
     * @param shapes the styling options for the Callout's container shape
     * @since 200.5.0
     */

    @Composable
    public fun Callout(
        location: Point,
        modifier: Modifier = Modifier,
        offset: Offset = Offset.Zero,
        rotateOffsetWithGeoView: Boolean = false,
        colorScheme: CalloutColors = CalloutDefaults.colors(),
        shapes: CalloutShapes = CalloutDefaults.shapes(),
        content: @Composable BoxScope.() -> Unit
    ): Unit = sceneViewScope.Callout(location, modifier, offset, rotateOffsetWithGeoView, colorScheme, shapes, content)

    /**
     * Creates a Callout at the specified [geoElement] or the [tapLocation] location on the MapView. The Callout is a composable
     * that can be used to display additional information about a location on the map. The additional information is
     * passed as a [content] composable that contains text and/or other content. It has a leader that points to
     * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
     * that contains the [content] lambda provided by the application. A thin border line is drawn around the entire Callout.
     *
     * If the given geoelement is a DynamicEntity then the Callout automatically updates its location everytime the
     * DynamicEntity changes. The content of the Callout however will not be automatically updated.
     *
     * Note: Only one Callout can be displayed at a time on the MapView.
     *
     * @param geoElement the GeoElement for which to display the Callout
     * @param modifier Modifier to be applied to the composable Callout
     * @param tapLocation a Point the user has tapped, or null if the Callout is not associated with a tap
     * @param colorScheme the styling options for the Callout's shape and color properties
     * @param shapes the styling options for the Callout's container shape
     * @param content the content of the Callout
     * @since 200.5.0
     */
    @Composable
    public fun Callout(
        geoElement: GeoElement,
        modifier: Modifier = Modifier,
        tapLocation: Point? = null,
        colorScheme: CalloutColors = CalloutDefaults.colors(),
        shapes: CalloutShapes = CalloutDefaults.shapes(),
        content: @Composable BoxScope.() -> Unit
    ): Unit = sceneViewScope.Callout(geoElement, modifier, tapLocation, colorScheme, shapes, content)
}