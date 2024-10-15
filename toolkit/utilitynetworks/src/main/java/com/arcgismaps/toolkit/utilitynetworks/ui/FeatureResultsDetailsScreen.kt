package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.sharp.ZoomIn
import androidx.compose.material.icons.sharp.ZoomInMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint
import com.arcgismaps.toolkit.utilitynetworks.TraceRun
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ExpandableCardWithLabel
import com.arcgismaps.toolkit.utilitynetworks.internal.util.TabRow
import com.arcgismaps.utilitynetworks.UtilityElement

@Composable
internal fun FeatureResultsDetailsScreen(
    traceRun: TraceRun,
    onFeatureSelected: (UtilityElement) -> Unit,
    onBackToNewTrace: () -> Unit,
    onBackToResults: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column {

            TabRow(onBackToNewTrace, 1)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.clickable { onBackToResults() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.feature_results),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            FeatureList(traceRun.featureResults, onFeatureSelected)
        }
    }
}

@Composable
private fun FeatureList(assetTypeList: List<UtilityElement>, onFeatureSelected: (UtilityElement) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column {
            ExpandableCardWithLabel(stringResource(R.string.attachement), value = assetTypeList.size.toString()) {
                Column {
                    assetTypeList.forEach { utilityElement ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                .clickable { onFeatureSelected(utilityElement) },
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.ZoomInMap,
                                contentDescription = "zoom in",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                modifier = Modifier.padding(start = 10.dp),
                                text = "Object ID: ${utilityElement.objectId}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
