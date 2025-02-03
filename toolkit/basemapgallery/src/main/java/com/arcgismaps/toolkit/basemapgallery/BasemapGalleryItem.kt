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

package com.arcgismaps.toolkit.basemapgallery

import android.graphics.Bitmap
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.portal.LoadableImage

/**
 * The BasemapGalleryItem encompasses an element in a [BasemapGallery].
 *
 * @property title the gallery item title
 * @property tag the object that this gallery item can return when clicked
 * @param thumbnailProvider a lambda that returns a bitmap to use for the thumbnail image. If the
 * lambda returns null a default thumbnail is used.
 * @constructor creates a gallery item
 */
public class BasemapGalleryItem(
    public val title: String, public val tag: Any? = null, internal val thumbnailProvider: suspend () -> Bitmap? = { null }
) {
    internal constructor(title: String, tag: Any?, thumbnail: LoadableImage?): this(title, tag, thumbnailProvider = {
        var bitmap: Bitmap? = null
        thumbnail?.load()?.onFailure { bitmap = null }?.onSuccess { bitmap = thumbnail.image?.bitmap }
        bitmap
    })

    /**
     * Constructors a ref@BasemapGalleryItem with a ref@Basemap.
     *
     * If the [BasemapStyleInfo] has a thumbnail, this is used for the thumbnail otherwise a default thumbnail is used.
     *
     * @param basemapStyleInfo the [BasemapStyleInfo]. If the [BasemapStyleInfo] has a thumbnail, this
     * is used for the thumbnail otherwise a default thumbnail is used.
     */
    public constructor(basemapStyleInfo: BasemapStyleInfo): this(basemapStyleInfo.styleName, basemapStyleInfo, basemapStyleInfo.thumbnail)
}
