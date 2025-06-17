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

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Produces the composable parts required to compose a [BasemapGalleryItem] in a [BasemapGallery].
 *
 * @param basemapGalleryItem the [BasemapGalleryItem]
 * @since 200.7.0
 */
@Composable
internal fun BasemapGalleryItem(
    basemapGalleryItem: BasemapGalleryItem,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val placeholder = painterResource(R.drawable.basemap)
    val thumbnail: MutableState<Painter> = remember { mutableStateOf(placeholder) }
    LaunchedEffect(thumbnail) {
        basemapGalleryItem.thumbnailProvider()?.let { bitmap ->
            bitmap.asImageBitmap().let { imageBitmap ->
                thumbnail.value = BitmapPainter(imageBitmap)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            )
            .padding(8.dp)
            .fillMaxSize()
    ) {
        Box {
            Image(
                painter = thumbnail.value,
                contentDescription = basemapGalleryItem.title,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            if (basemapGalleryItem.is3D) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("3D")
                }
            }
        }
        Text(text = basemapGalleryItem.title, textAlign = TextAlign.Center)
    }
}

/**
 * A gallery of [BasemapGalleryItem]s.
 *
 * @param basemapGalleryItems the items to show in the gallery
 * @param onItemClick a lambda to execute when a gallery item is clicked
 * @param modifier the modifier to apply to this gallery
 * @since 200.7.0
 */
@Composable
public fun BasemapGallery(
    basemapGalleryItems: List<BasemapGalleryItem>,
    onItemClick: (BasemapGalleryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selection by rememberSaveable { mutableIntStateOf(-1) }

    LazyVerticalGrid(modifier = modifier, columns = GridCells.Adaptive(minSize = 128.dp)) {
        basemapGalleryItems.forEachIndexed { index, basemapGalleryItem ->
            item {
                BasemapGalleryItem(
                    basemapGalleryItem,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            selection = index
                            onItemClick(basemapGalleryItem)
                        },
                    index == selection
                )
            }
        }
    }
}

/**
 * A preview of the [BasemapGallery].
 *
 * @since 200.7.0
 */
@Preview(showBackground = true)
@Composable
internal fun BasemapGalleryPreview() {
    val items = mutableListOf<BasemapGalleryItem>()
    for (i in 0..100) {
        items.add(BasemapGalleryItem(title = "Item $i", is3D = (i < 10)))
    }
    BasemapGallery(
        items,
        onItemClick = {
            Log.d("BasemapGallery", "Item clicked: ${it.title}")
        }
    )
}
