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

package com.arcgismaps.toolkit.basemapgalleryapp

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

class ViewModel(application: Application) : AndroidViewModel(application) {

//    private val _items: MutableList<BasemapGalleryItem> = mutableListOf()
//
//    val items: List<BasemapGalleryItem>
//        get() = _items

    var items = mutableStateListOf<BasemapGalleryItem>()
        private set

    val mapViewProxy = MapViewProxy()
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery)

    init {
        viewModelScope.launch {
//            val portal = Portal("https://www.arcgis.com")
//            portal.load().getOrThrow()
//            val result = portal.fetchBasemaps().getOrThrow()
//
//            result.forEach {
//                Log.d("BasemapGallery", "${it.item?.itemId}")
//
//                val item = it.item
//
//                item?.thumbnail?.load()?.getOrThrow()
//
//                val image = item?.thumbnail?.image?.bitmap?.asImageBitmap()
//                val painter = BitmapPainter(image!!)
//                val title = item.title
//
//                val galleryItem = BasemapGalleryItem(title, thumbnail = painter, tag = item)
//                items.add(galleryItem)
//            }

            val service = BasemapStylesService()

            service.load().getOrThrow()

            service.info?.stylesInfo?.forEach {
                it.thumbnail?.load()?.getOrThrow()

                val image = it.thumbnail?.image?.bitmap?.asImageBitmap()
                val painter = BitmapPainter(image!!)
                val title = it.styleName

                val galleryItem = BasemapGalleryItem(title, thumbnail = painter, tag = it)
                items.add(galleryItem)
            }
        }
    }
}