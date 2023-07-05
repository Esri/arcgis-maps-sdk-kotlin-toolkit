package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapListScreen(
    modifier: Modifier = Modifier,
    mapListViewModel: MapListViewModel = viewModel(),
    onItemClick: (String) -> Unit = {}
) {
    val isLoading by mapListViewModel.isLoading.collectAsState()
    val portalItems = mapListViewModel.portalItems
    val lazyListState = rememberLazyListState()

    Scaffold(topBar = {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = "Maps",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
            )
        }
    }) { paddingValues ->

        Crossfade(
            targetState = isLoading,
            modifier = Modifier.padding(paddingValues)
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
                            portalItem = it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            onItemClick(it.url)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapListItem(
    portalItem: PortalItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        portalItem.thumbnail?.image?.bitmap?.asImageBitmap()?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop
            )
        }
        // if thumbnail from the portalItem is empty then use the default
        // map placeholder
            ?: Image(
                painter = painterResource(id = R.drawable.ic_default_map),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit
            )

        Spacer(modifier = Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = portalItem.title, style = MaterialTheme.typography.bodyLarge)
            val lastUpdated =
                Text(
                    text = "Last Updated: ${portalItem.modified?.format("MMM dd yyyy")}",
                    style = MaterialTheme.typography.labelSmall
                )
        }
    }
}

fun Instant.format(format: String): String {
    return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(this)
}
