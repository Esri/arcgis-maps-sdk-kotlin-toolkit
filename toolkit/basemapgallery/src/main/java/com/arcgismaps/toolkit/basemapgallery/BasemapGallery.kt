/*
 *
 *  Copyright 2024 Esri
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

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun item(basemapGalleryItem: BasemapGalleryItem) {
    val placeholder = painterResource(R.drawable.basemap)
    val thumbnail: MutableState<Painter> = remember { mutableStateOf(placeholder) }
    LaunchedEffect(thumbnail) {
        basemapGalleryItem.thumbnailProvider(basemapGalleryItem)?.let {bitmap ->
            bitmap.asImageBitmap().let {imageBitmap ->
                thumbnail.value = BitmapPainter(imageBitmap)
            }
        }
        thumbnail
    }
    Image(
        painter = thumbnail.value,
        contentDescription = basemapGalleryItem.title
    )
    Text(text = basemapGalleryItem.title)
}

@Composable
public fun BasemapGallery(basemapGalleryItems: List<BasemapGalleryItem>, modifier: Modifier = Modifier, onItemClick: (BasemapGalleryItem) -> Unit) {
    LazyVerticalGrid(modifier = modifier, columns = GridCells.Adaptive(128.dp)) {
        basemapGalleryItems.forEach { basemapGalleryItem ->
            item {
                Column(modifier = Modifier
                    .padding(8.dp)
                    .clickable { onItemClick(basemapGalleryItem) }) {
                    item(basemapGalleryItem)
                }
            }
        }
    }
}

@Preview
@Composable
internal fun BasemapGalleryPreview() {
    val items = mutableListOf<BasemapGalleryItem>()
    for (i in 0..100) {
        items.add(BasemapGalleryItem(title = "Item $i"))
    }
    BasemapGallery(items, onItemClick = {
        Log.d("BaseMapGallery", "Item clicked: ${it.title}")
    })
}
