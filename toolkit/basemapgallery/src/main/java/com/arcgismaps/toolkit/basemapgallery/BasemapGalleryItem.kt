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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.arcgismaps.mapping.BasemapStyleInfo
import com.arcgismaps.mapping.Item
import com.arcgismaps.portal.LoadableImage

public class BasemapGalleryItem private constructor(public val title: String,
                                public val tag: Any? = null,
                                internal val painterProvider: @Composable () -> MutableState<Painter>
) {
    public constructor(title: String,
                       tag: Any?,
                       loadableImage: LoadableImage?,
                       placeholderProvider: @Composable () -> Painter) : this(title, tag, painterProvider = {
        val placeholder = placeholderProvider()
        val thumbnail: MutableState<Painter> = remember { mutableStateOf(placeholder) }

        LaunchedEffect(thumbnail) {
            loadableImage?.let { image ->
                image.load().onSuccess {
                    image.image?.bitmap?.asImageBitmap()?.let {
                        thumbnail.value = BitmapPainter(it)
                    }
                }
            }
        }

        thumbnail
    })

    public constructor(basemapStyleInfo: BasemapStyleInfo, placeholderProvider: @Composable () -> Painter) : this(
        basemapStyleInfo.styleName, tag = basemapStyleInfo, loadableImage = basemapStyleInfo.thumbnail, placeholderProvider = placeholderProvider)

    public constructor(item: Item, placeholderProvider: @Composable () -> Painter) : this(
        item.title, tag = item, loadableImage = item.thumbnail, placeholderProvider = placeholderProvider)

//    public constructor(title: String,
//                       tag: Any?,
//                       thumbnailProvider: @Composable () -> Painter): this(title, tag, painterProvider = {
//        val provider = thumbnailProvider()
//        remember { mutableStateOf(provider) }
//    })
}
