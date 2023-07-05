package com.arcgismaps.toolkit.featureformsapp.screens.browse

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.toolkit.featureformsapp.R

@Composable
fun MapListScreen(
    mapList: List<String>,
    onItemClick: (String) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(15.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(mapList) {
            MapListItem(uri = it) {
                onItemClick(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapListItem(
    uri: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val portal = remember {
        PortalItem(uri)
    }
    var loaded by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(portal) {
        portal.load().onSuccess {
            portal.thumbnail?.load()
            loaded = true
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(modifier = Modifier.size(150.dp)) {
                if (loaded) {
                    portal.thumbnail?.image?.bitmap?.asImageBitmap()
                        ?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_default_map),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            if (loaded) {
                Text(text = portal.title)
            }
        }
    }
}

fun getListOfMaps(context: Context): List<String> {
    return listOf(
        context.getString(R.string.map_url_orig),
        context.getString(R.string.map_url_text_box_area),
        context.getString(R.string.map_url_visibility),
        context.getString(R.string.map_url_editable),
        context.getString(R.string.map_url_length_default_value),
        context.getString(R.string.map_url_range_domain_combo),
        context.getString(R.string.map_url_all_text_state)
    )
}
