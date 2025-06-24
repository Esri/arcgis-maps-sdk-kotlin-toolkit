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

/**
 * A point of interest that users of the ArFlyoverApp can zoom to.
 *
 * @property name the name of this POI
 * @property description a description of the POI, for display in a Callout
 * @property x the WGS84 x-coordinate of the location of this POI
 * @property y the WGS84 y-coordinate of the location of this POI
 * @property z the WGS84 z-coordinate of the location of this POI
 * @property heading the initial Camera heading for this POI
 * @property translationFactor the translation factor to use for this POI
 * @since 200.8.0
 */
data class PointOfInterest(
    val name: String, val description: String, val x: Double, val y: Double, val z: Double,
    val heading: Double, val translationFactor: Double)

val pointsOfInterestList = listOf(
    PointOfInterest("Flying over the city", "xxx", 2.82407, 41.99101,230.0, 160.0, 1000.0),
    PointOfInterest("Girona Cathedral", "xxx", 2.8246561632960923,41.987190133522844, 110.50172981619835, 80.87130465869643, 10.0),
    PointOfInterest("On the city walls", "xxx", 2.828377064683506, 41.98670727904231, 147.3971471907571, 175.8728875641343, 10.0),
    PointOfInterest("By the river", "xxx", 2.824210487810305, 41.98506243493622, 98.24900419544429, 202.36680611861564, 10.0),
)
