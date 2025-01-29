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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.BasemapStylesService
import com.arcgismaps.toolkit.basemapgallery.BasemapGalleryItem
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.launch

class ViewModel(application: Application) : AndroidViewModel(application) {

    var items = mutableStateListOf<BasemapGalleryItem>()
        private set

    val mapViewProxy = MapViewProxy()
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery)

    init {
        viewModelScope.launch {
            // set up a placeholder bitmap
//            val colors = IntArray(100 * 100)
//            for (i in 0..<100 * 100) {
//                colors[i] = 0xFF0000FF.toInt()
//            }
//            val bitmap = Bitmap.createBitmap(colors, 100, 100, Bitmap.Config.ARGB_8888)
//                .asImageBitmap()
//            val placeholder = BitmapPainter(bitmap)

//            // get basemaps from a portal
//            val portal = Portal("https://www.arcgis.com")
//            portal.load().getOrThrow()
//            val basemaps = portal.fetchBasemaps().getOrThrow()
//
//            basemaps.forEach { basemap ->
//                basemap.item?.let {item ->
//                    val galleryItem = BasemapGalleryItem(title = item.title,
//                        loadableImage = item.thumbnail, placeholderProvider = { painterResource(R.drawable.coin) }, tag = item)
//                    items.add(galleryItem)
//                }
//            }

            // get basemaps from a basemap style service
            val service = BasemapStylesService()
            service.load().getOrThrow()

            service.info?.stylesInfo?.forEach {basemapStyleInfo ->
                val galleryItem = BasemapGalleryItem(title = basemapStyleInfo.styleName,
                    tag = basemapStyleInfo, loadableImage = basemapStyleInfo.thumbnail,
                    placeholderProvider = { painterResource(R.drawable.basemap) })
                items.add(galleryItem)
            }
        }
    }
}