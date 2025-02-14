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

package com.arcgismaps.toolkit.basemapgallery

import android.graphics.Bitmap
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.mapping.Item
import com.arcgismaps.portal.LoadableImage

/**
 * The BasemapGalleryItem encompasses an element in a [BasemapGallery].
 *
 * @property title the gallery item title
 * @property tag the object that this gallery item can return when clicked
 * @param thumbnailProvider a lambda that returns a bitmap to use for the thumbnail image. If the
 * lambda returns null a default thumbnail is used.
 * @constructor creates a gallery item
 * @since 200.7.0
 */
public class BasemapGalleryItem(
    public val title: String,
    public val tag: Any? = null,
    internal val thumbnailProvider: suspend () -> Bitmap? = { null }
) {
    internal constructor(title: String, tag: Any?, thumbnail: LoadableImage?) : this(
        title,
        tag,
        thumbnailProvider = {
            var bitmap: Bitmap? = null
            thumbnail?.load()?.onSuccess { bitmap = thumbnail.image?.bitmap }
            bitmap
        })

    /**
     * Construct a [BasemapGalleryItem] with a [BasemapStyleInfo].
     *
     * If the [BasemapStyleInfo] has a thumbnail, this is used for the thumbnail otherwise a default thumbnail is used.
     *
     * @param basemapStyleInfo the [BasemapStyleInfo]
     * @since 200.7.0
     */
    public constructor(basemapStyleInfo: BasemapStyleInfo) : this(
        basemapStyleInfo.styleName,
        basemapStyleInfo,
        basemapStyleInfo.thumbnail
    )

    /**
     * Construct a [BasemapGalleryItem] with an [Item].
     *
     * If the [Item] has a thumbnail, this is used for the thumbnail otherwise a default thumbnail is used.
     *
     * @param item the [Item]
     * @since 200.7.0
     */
    public constructor(item: Item) : this(
        item.title,
        item,
        item.thumbnail
    )
}
