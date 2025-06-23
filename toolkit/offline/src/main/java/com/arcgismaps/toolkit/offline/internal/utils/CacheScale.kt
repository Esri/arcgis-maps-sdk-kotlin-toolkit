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

package com.arcgismaps.toolkit.offline.internal.utils

import androidx.annotation.StringRes
import com.arcgismaps.arcgisservices.LevelOfDetail
import com.arcgismaps.toolkit.offline.R

/**
 * An enumeration of predefined cache scales, each corresponding to a specific
 * level of detail.
 *
 * @param descriptionResId The string resource ID for the description of the cache scale.
 * @param levelOfDetail The [LevelOfDetail] object containing the level, resolution, and scale.
 * @since 200.8.0
 */
internal enum class CacheScale(
    @StringRes val descriptionResId: Int,
    private val levelOfDetail: LevelOfDetail? = null
) {
    ROOM(
        R.string.room,
        LevelOfDetail(level = 23, resolution = 0.01866138385297604, scale = 70.5310735)
    ),
    ROOMS(
        R.string.rooms,
        LevelOfDetail(level = 22, resolution = 0.03732276770595208, scale = 141.062147)
    ),
    HOUSE_PROPERTY(
        R.string.house_property,
        LevelOfDetail(level = 21, resolution = 0.07464553541190416, scale = 282.124294)
    ),
    HOUSES(
        R.string.houses,
        LevelOfDetail(level = 20, resolution = 0.14929107082380833, scale = 564.248588)
    ),
    SMALL_BUILDING(
        R.string.small_building,
        LevelOfDetail(level = 19, resolution = 0.2985821417799086, scale = 1128.497175)
    ),
    BUILDING(
        R.string.building,
        LevelOfDetail(level = 18, resolution = 0.5971642835598172, scale = 2256.994353)
    ),
    BUILDINGS(
        R.string.buildings,
        LevelOfDetail(level = 17, resolution = 1.1943285668550503, scale = 4513.988705)
    ),
    STREET(
        R.string.street,
        LevelOfDetail(level = 16, resolution = 2.388657133974685, scale = 9027.977411)
    ),
    STREETS(
        R.string.streets,
        LevelOfDetail(level = 15, resolution = 4.77731426794937, scale = 18055.954822)
    ),
    NEIGHBORHOOD(
        R.string.neighborhood,
        LevelOfDetail(level = 14, resolution = 9.554628535634155, scale = 36111.909643)
    ),
    TOWN(
        R.string.town,
        LevelOfDetail(level = 13, resolution = 19.10925707126831, scale = 72223.819286)
    ),
    CITY(
        R.string.city,
        LevelOfDetail(level = 12, resolution = 38.21851414253662, scale = 144447.638572)
    ),
    CITIES(
        R.string.cities,
        LevelOfDetail(level = 11, resolution = 76.43702828507324, scale = 288895.277144)
    ),
    METROPOLITAN_AREA(
        R.string.metropolitan_area,
        LevelOfDetail(level = 10, resolution = 152.87405657041106, scale = 577790.554289)
    ),
    COUNTY(
        R.string.county,
        LevelOfDetail(level = 9, resolution = 305.74811314055756, scale = 1155581.108577)
    ),
    COUNTIES(
        R.string.counties,
        LevelOfDetail(level = 8, resolution = 611.4962262813797, scale = 2311162.217155)
    ),
    STATE_PROVINCE(
        R.string.state_province,
        LevelOfDetail(level = 7, resolution = 1222.992452562495, scale = 4622324.434309)
    ),
    STATES_PROVINCES(
        R.string.states_provinces,
        LevelOfDetail(level = 6, resolution = 2445.98490512499, scale = 9244648.868618)
    ),
    COUNTRIES_SMALL(
        R.string.countries_small,
        LevelOfDetail(level = 5, resolution = 4891.96981024998, scale = 18489297.737236)
    ),
    COUNTRIES_BIG(
        R.string.countries_big,
        LevelOfDetail(level = 4, resolution = 9783.93962049996, scale = 36978595.474472)
    ),
    CONTINENT(
        R.string.continent,
        LevelOfDetail(level = 3, resolution = 19567.87924099992, scale = 73957190.948944)
    ),
    WORLD_SMALL(
        R.string.world_small,
        LevelOfDetail(level = 2, resolution = 39135.75848200009, scale = 147914381.897889)
    ),
    WORLD_BIG(
        R.string.world_big,
        LevelOfDetail(level = 1, resolution = 78271.51696399994, scale = 295828763.795777)
    ),
    WORLD(
        R.string.world,
        LevelOfDetail(level = 0, resolution = 156543.03392800014, scale = 591657527.591555)
    );

    /**
     * The scale value of the level of detail.
     */
    val scale: Double
        get() = this.levelOfDetail?.scale ?: 0.0

    /**
     * A string representation of the scale, formatted as "1:<scale>".
     */
    val scaleDescription: String
        get() = "1:${scale.toInt()}"
}
