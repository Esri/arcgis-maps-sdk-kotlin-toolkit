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

public class BasemapGalleryItem(
    public val title: String, public val tag: Any? = null, internal val thumbnailProvider: suspend () -> Bitmap? = { null }
) {
    internal constructor(title: String, tag: Any?, thumbnail: LoadableImage?): this(title, tag, thumbnailProvider = {
        var bitmap: Bitmap? = null
        thumbnail?.load()?.onFailure { bitmap = null }?.onSuccess { bitmap = thumbnail.image?.bitmap }
        bitmap
    })

    public constructor(basemapStyleInfo: BasemapStyleInfo): this(basemapStyleInfo.styleName, basemapStyleInfo, basemapStyleInfo.thumbnail)
}
