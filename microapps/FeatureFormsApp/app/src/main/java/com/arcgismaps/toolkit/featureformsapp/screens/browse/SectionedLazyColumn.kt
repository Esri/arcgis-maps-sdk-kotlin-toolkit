/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.LoadableImage
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.data.datastore
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

@Composable
fun SectionedLazyColumn(
    portalFolders: List<PortalFolder>,
    portalItems: List<PortalItem>,
    modifier: Modifier = Modifier,
    onFolderClick: (PortalFolder) -> Unit,
    onItemClick: (PortalItem) -> Unit,
    isInitiallyExpanded: Boolean = false
) {
    var isFolderViewExpanded by rememberSaveable { mutableStateOf(isInitiallyExpanded) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val folders by remember {
        derivedStateOf {
            if (isFolderViewExpanded) {
                portalFolders
            } else {
                emptyList()
            }
        }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        state = lazyListState
    ) {
        if (portalFolders.count() > 1) {
            stickyHeader {
                ExpandableHeader(
                    title = "Folders",
                    count = portalFolders.size,
                    modifier = Modifier
                        .fillMaxWidth(),
                    onExpanded = { expanded ->
                        isFolderViewExpanded = !isFolderViewExpanded
                        scope.launch {
                            if (folders.isEmpty()) {
                                lazyListState.requestScrollToItem(0)
                            }
                        }
                    },
                    expandable = true
                )
            }
            itemsIndexed(folders) { index, folder ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    folders.lastIndex -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                if (index == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                FolderItem(
                    name = folder.title,
                    onClick = { onFolderClick(folder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 12.dp)
                        .clip(shape)
                )
                if (index != folders.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        stickyHeader {
            ExpandableHeader(
                title = "Maps",
                count = portalItems.size,
                modifier = Modifier.fillMaxWidth(),
                expandable = false
            )
        }
        items(portalItems) { item ->
            MapListItem(
                title = item.title,
                lastModified = item.modified,
                shareType = item.access.encoding.uppercase(Locale.getDefault()),
                thumbnail = item.thumbnail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                onItemClick(item)
            }
        }
    }
}

@Composable
fun ExpandableHeader(
    title: String,
    count : Int,
    modifier: Modifier = Modifier,
    expandable: Boolean,
    onExpanded: ((Boolean) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = expandable) {
                expanded = !expanded
                onExpanded?.invoke(expanded)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.titleSmall,
            )
            if (expandable) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier
                        //.padding(8.dp)
                        .size(24.dp)
                )
            }
        }
    }
}

/**
 * A list item row for a PortalItem that shows the [title], [lastModified] and thumbnail. Provides
 * an [onClick] callback when the item is tapped.
 */
@Composable
fun MapListItem(
    title: String,
    lastModified: Instant?,
    shareType: String,
    thumbnail: LoadableImage?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val placeholder = painterResource(id = R.drawable.ic_default_map)
    Row(
        modifier = modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Box {
            MapListItemThumbnail(
                loadableImage = thumbnail,
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(16 / 9f)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .wrapContentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = shareType,
                    modifier = Modifier.padding(5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            lastModified?.let {
                Text(
                    text = "Last Updated: ${it.format("MMM dd yyyy")}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun MapListItemThumbnail(
    loadableImage: LoadableImage?,
    placeholder: Painter,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val scope = rememberCoroutineScope()
    loadableImage?.let {
        val imageLoader = remember {
            ImageLoader(
                loadable = it,
                scope = scope,
                placeholder = placeholder,
            )
        }
        AsyncImage(
            imageLoader = imageLoader,
            modifier = modifier,
            contentScale = contentScale
        )
    } ?: Image(
        painter = placeholder,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun FolderItem(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painterResource(R.drawable.folder_m3),
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Text(
                text = name,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun FolderItemPreview() {
    FolderItem(
        name = "Sample Folder",
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MapListItemPreview() {
    MapListItem(
        title = "Water Utility",
        lastModified = Instant.ofEpochMilli(1685628000000),
        shareType = "Public",
        modifier = Modifier.size(width = 485.dp, height = 100.dp),
        thumbnail = null,
    )
}
