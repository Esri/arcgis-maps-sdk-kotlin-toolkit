package com.arcgismaps.toolkit.offlinemapareasapp.screens.browse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.OfflineMapInfo
import com.arcgismaps.toolkit.offline.OfflineRepository


@Composable
fun OnDeviceMapInfo(
    onClick: (String) -> Unit,
    offlineMapInfos: List<OfflineMapInfo>,
    offlineRepository: OfflineRepository,
) {
    val context = LocalContext.current
    LazyColumn {
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                enabled = offlineMapInfos.isNotEmpty(),
                onClick = {
                    offlineRepository.removeAllDownloads(context)
                }) {
                Text("Remove all downloads")
            }
        }
        offlineMapInfos.forEach { offlineMapInfo ->
            item {
                OfflineMapInfoCard(
                    info = offlineMapInfo,
                    onOpen = {
                        onClick.invoke(offlineMapInfo.id)
                    },
                    onDelete = {
                        offlineRepository.removeDownloadsForWebmap(context, offlineMapInfo)
                    })
            }
        }
    }
}

@Composable
fun OfflineMapInfoCard(
    info: OfflineMapInfo,
    modifier: Modifier = Modifier,
    placeholder: Bitmap = rememberInfoPlaceholderBitmap(),
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onOpen.invoke() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bmp = info.thumbnail ?: placeholder
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Map thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete map"
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open map"
                )
            }
        }
    }
}


@Composable
fun rememberInfoPlaceholderBitmap(): Bitmap {
    val resources = LocalContext.current.resources
    return remember {
        BitmapFactory.decodeResource(
            resources,
            android.R.drawable.ic_dialog_info
        )
    }
}
