package com.arcgismaps.toolkit.offlinemapareasapp.screens.browse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.OfflineMapInfo
import com.arcgismaps.toolkit.offline.OfflineRepository


@Composable
fun OnDeviceMapInfo(
    onClick: (String) -> Unit, offlineMapInfos: List<OfflineMapInfo>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        offlineMapInfos.forEach { offlineMapInfo ->
            item {
                OfflineMapInfoCard(info = offlineMapInfo, onOpen = {
                    onClick.invoke(offlineMapInfo.id)
                }, onDelete = {
                    OfflineRepository.removeDownloadsForWebmap(context, offlineMapInfo)
                })
            }
        }
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Offline map infos will be displayed here when downloads have completed.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                onClick = { OfflineRepository.refreshOfflineMapInfos(context) }) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(4.dp))
                Text("Refresh: Offline map infos")
            }
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                enabled = offlineMapInfos.isNotEmpty(),
                onClick = { OfflineRepository.removeAllDownloads(context) }) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(4.dp))
                Text("Remove all downloads")
            }
        }
    }
}

@Composable
fun OfflineMapInfoCard(
    info: OfflineMapInfo,
    placeholder: Bitmap = rememberInfoPlaceholderBitmap(),
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bmp = info.thumbnail ?: placeholder
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Map thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = info.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete map",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open map",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
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
            resources, android.R.drawable.ic_dialog_info
        )
    }
}
