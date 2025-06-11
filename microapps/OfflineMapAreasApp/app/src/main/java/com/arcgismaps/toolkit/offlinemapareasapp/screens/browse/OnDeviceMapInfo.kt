package com.arcgismaps.toolkit.offlinemapareasapp.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.offline.OfflineMapInfo
import com.arcgismaps.toolkit.offline.OfflineRepository


@Composable
fun OnDeviceMapInfo(
    onClick: (String) -> Unit,
    offlineMapInfos: List<OfflineMapInfo>,
    offlineRepository: OfflineRepository
) {
    LazyColumn {
        offlineRepository.offlineMapInfos.forEach { offlineMapInfo ->
            item {
                Text(
                    modifier = Modifier.clickable { onClick.invoke(offlineMapInfo.id) },
                    text = offlineMapInfo.title
                )
            }
        }
    }
}