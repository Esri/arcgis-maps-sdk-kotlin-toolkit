/*
 *
 *  Copyright 2025 Esri
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
package com.arcgismaps.toolkit.arflyoverapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference

/**
 * A Point of Interest that users of the ArFlyoverApp can zoom to.
 *
 * @property name the name of this POI
 * @property description a description of the POI, for display in a Callout
 * @property poiLocation the location of this POI
 * @property calloutLocation the location at which to display the Callout for this POI, or null if
 * there is no Callout for this POI
 * @property heading the initial Camera heading for this POI
 * @property translationFactor the translation factor to use for this POI
 * @since 200.8.0
 */
data class PointOfInterest(
    val name: String, val description: String, val poiLocation: Point, val calloutLocation: Point?,
    val heading: Double, val translationFactor: Double
)

/**
 * Create and remember a list of Points of Interest to offer to the user.
 *
 * @since 200.8.0
 */
@Composable
fun rememberPointsOfInterest(): List<PointOfInterest> = remember {
    listOf(
        PointOfInterest(
            name = "Flying over the city",
            description = "",
            poiLocation = Point(2.82407, 41.99101, 230.0, SpatialReference.wgs84()),
            calloutLocation = null,
            heading = 160.0,
            translationFactor = 1000.0
        ),
        PointOfInterest(
            name = "Girona Cathedral",
            description =
                "Here we see Girona Cathedral, parts of which date back to the 12th Century.",
            poiLocation = Point(2.8246561632960923, 41.987190133522844, 110.50172981619835,
                SpatialReference.wgs84()),
            calloutLocation = Point(2.8260033615157907, 41.98737627492155, 90.67858890071511,
                SpatialReference.wgs84()),
            heading = 80.87130465869643,
            translationFactor = 10.0
        ),
        PointOfInterest(
            name = "On the city walls",
            description =
                "Here you can explore Girona's city walls, built in Roman and medieval times, starting at the old fortress of Torre Gironella.",
            poiLocation = Point(2.828377064683506, 41.98670727904231, 147.3971471907571,
                SpatialReference.wgs84()),
            calloutLocation = Point(2.828444350938064, 41.98619391066322, 110.8261399846524,
                SpatialReference.wgs84()),
            heading = 175.8728875641343,
            translationFactor = 50.0
        ),
        PointOfInterest(
            name = "By the river",
            description =
                "Initially, looking South, you'll see the Pont de les Peixateries Velles, an iron bridge built by Gustave Eiffel's company in 1876. Turn to look North and you'll see the colourful Cases de l'Onyar on your right and the historic old town behind them.",
            poiLocation = Point(2.824210487810305, 41.98506243493622, 98.24900419544429,
                SpatialReference.wgs84()),
            calloutLocation = Point(2.823699820709328, 41.98434765729695, 64.07990946341306,
                SpatialReference.wgs84()),
            heading = 202.36680611861564,
            translationFactor = 20.0
        )
    )
}
