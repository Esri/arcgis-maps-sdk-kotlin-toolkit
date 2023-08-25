package com.arcgismaps.toolkit.featureformsapp.screens.browse

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.portal.LoadableImage
import com.arcgismaps.portal.PortalAccess
import com.arcgismaps.toolkit.featureformsapp.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a list of PortalItems using the [mapListViewModel]. Provides a callback [onItemClick]
 * when an item is tapped.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MapListScreen(
    modifier: Modifier = Modifier,
    mapListViewModel: MapListViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit = {}
) {
    val uiState by mapListViewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    Scaffold(topBar = {
        AppBar {
            mapListViewModel.refresh()
        }
    }) { padding ->
        // use a cross fade animation to show a loading indicator when the data is loading
        // and transition to the list of portalItems once loaded
        AnimatedContent(
            targetState = uiState.isLoading,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                fadeIn(animationSpec = tween(1000)) with fadeOut()
            }
        ) { state ->
            when (state) {
                true -> Box(modifier = modifier.fillMaxSize()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Loading...",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                false -> if (uiState.data.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = lazyListState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.data) { item ->
                            MapListItem(
                                title = item.portalItem.title,
                                lastModified = item.portalItem.modified?.format("MMM dd yyyy")
                                    ?: "",
                                iconDrawable = if (item.portalItem.access == PortalAccess.Public) R.drawable.ic_public
                                else R.drawable.ic_private,
                                thumbnail = item.portalItem.thumbnail, //?.image?.bitmap?.asImageBitmap(),
                                imageLoader = mapListViewModel.imageLoader,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) {
                                onItemClick(item.portalItem.url)
                            }
                        }
                    }
                } else if (!uiState.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Log.e("TAG", "MapListScreen: nothing to show")
                        Text(text = "Nothing to show. Pull-down to Refresh")
                    }
                }
            }
        }
    }
}


/**
 * A list item row for a PortalItem that shows the [title], [lastModified] and [thumbnail]. Provides
 * an [onClick] callback when the item is tapped.
 */
@Composable
fun MapListItem(
    title: String,
    lastModified: String,
    iconDrawable: Int,
    modifier: Modifier = Modifier,
    thumbnail: LoadableImage? = null,
    imageLoader : suspend (LoadableImage) -> Unit,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Box {
            MapItemThumbnail(thumbnail = thumbnail, imageLoader)
            Image(
                painterResource(id = iconDrawable),
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp)
                    .size(25.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Last Updated: $lastModified", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MapItemThumbnail(thumbnail : LoadableImage?, loader : suspend (LoadableImage) -> Unit) {
    var loadedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    loadedImage?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(15.dp)),
            contentScale = ContentScale.Crop
        )
    }
    // if thumbnail is empty then use the default map placeholder
        ?: Image(
            painter = painterResource(id = R.drawable.ic_default_map),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(15.dp)),
            contentScale = ContentScale.Crop
        )
    LaunchedEffect(thumbnail) {
        thumbnail?.let {
            loader(it)
            loadedImage = it.image?.bitmap?.asImageBitmap()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(onRefresh: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = "Maps",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Image(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(150.dp),
                offset = DpOffset((15).dp, 0.dp)
            ) {
                DropdownMenuItem(text = { Text(text = "Refresh") }, onClick = {
                    expanded = false
                    onRefresh()
                })
            }
        }
    )
}

/**
 * Utility function to convert an Instant into a string based on [format]
 */
fun Instant.format(format: String): String =
    DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(this)

@Preview
@Composable
fun MapListItemPreview() {
    MapListItem(
        title = "Water Utility",
        lastModified = "June 1 2023",
        iconDrawable = R.drawable.ic_public,
        modifier = Modifier.size(width = 485.dp, height = 100.dp),
        imageLoader = {}
    )
}
