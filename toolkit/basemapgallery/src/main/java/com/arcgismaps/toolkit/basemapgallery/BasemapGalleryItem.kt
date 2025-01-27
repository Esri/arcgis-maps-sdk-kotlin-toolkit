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

import androidx.compose.ui.graphics.painter.Painter

public class BasemapGalleryItem(public val title: String,
                                public val tag: Any? = null,
                                internal val thumbnail: Painter,
                                //internal val image: @Composable () -> ImageBitmap?
)
