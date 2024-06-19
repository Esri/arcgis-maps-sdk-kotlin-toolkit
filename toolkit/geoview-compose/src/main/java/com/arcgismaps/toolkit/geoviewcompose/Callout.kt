/*
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.GeoView

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
 * @since 200.5.0
 */
@Composable
public fun GeoViewScope.Callout(
    location: Point,
    modifier: Modifier = Modifier,
    offset: Offset = Offset.Zero,
    rotateOffsetWithGeoView: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    if (!this.isCalloutBeingDisplayed) {
        isCalloutBeingDisplayed = true
        this.Callout(location, modifier, offset, rotateOffsetWithGeoView, content)
    }
}
