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

package com.arcgismaps.toolkit.offline

import android.graphics.Bitmap
import com.arcgismaps.mapping.PortalItem

/**
 * Represents the information of an offline map.
 *
 * @param portalItem The [PortalItem] representing the offline map.
 * @since 200.8.0
 */
public class OfflineMapInfo(private val portalItem: PortalItem) {

    public val portalItemUrl: String
        get() = portalItem.portal.url

    public val title: String
        get() = portalItem.title

    public val description: String
        get() = portalItem.description

    public var thumbnail: Bitmap? = null

    public suspend fun initialize(): Result<Unit> {
        return runCatchingCancellable {
            portalItem.load().getOrNull()
            portalItem.thumbnail?.load()?.getOrElse {
                thumbnail = null
                return Result.failure(it)
            }
            thumbnail = portalItem.thumbnail?.image?.bitmap
        }
    }
}
