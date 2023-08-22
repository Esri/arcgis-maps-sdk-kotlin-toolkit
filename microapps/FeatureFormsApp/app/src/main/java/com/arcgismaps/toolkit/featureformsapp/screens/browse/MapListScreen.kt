package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.data.DataSourceType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a list of PortalItems using the [mapListViewModel]. Provides a callback [onItemClick]
 * when an item is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapListScreen(
    modifier: Modifier = Modifier,
    mapListViewModel: MapListViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit = {}
) {
    val portalItems by mapListViewModel.portalItems.collectAsState(emptyList())
    val lazyListState = rememberLazyListState()

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "Maps",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
        )
    }) { padding ->
        // use a cross fade animation to show a loading indicator when the data is loading
        // and transition to the list of portalItems once loaded
        Crossfade(
            targetState = portalItems.isEmpty(),
            modifier = Modifier.padding(padding)
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

                false -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = lazyListState,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(portalItems) {
                        MapListItem(
                            title = it.portalItem.title,
                            lastModified = it.portalItem.modified?.format("MMM dd yyyy") ?: "",
                            iconDrawable = if (it.itemData.type == DataSourceType.Local) R.drawable.ic_public
                            else R.drawable.ic_private,
                            thumbnail = it.portalItem.thumbnail?.image?.bitmap?.asImageBitmap(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            onItemClick(it.portalItem.url)
                        }
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
    thumbnail: ImageBitmap? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Box {
            thumbnail?.let {
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
            Image(
                painterResource(id = iconDrawable),
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp).size(25.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Last Updated: $lastModified", style = MaterialTheme.typography.labelSmall)
        }
    }
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
        modifier = Modifier.size(width = 485.dp, height = 100.dp)
    )
}
