package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ZoomInMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.internal.util.ExpandableCardWithLabel
import com.arcgismaps.toolkit.utilitynetworks.internal.util.TabRow
import com.arcgismaps.toolkit.utilitynetworks.internal.util.UpButton
import com.arcgismaps.utilitynetworks.UtilityElement

@Composable
internal fun FeatureResultsDetailsScreen(
    selectedGroupName: String,
    elementListWithSelectedGroupName: List<UtilityElement>,
    onFeatureSelected: (UtilityElement) -> Unit,
    onBackToNewTrace: () -> Unit,
    onBackToResults: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column {

            TabRow(onBackToNewTrace, 1)

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
            )

            UpButton(stringResource(id = R.string.feature_results), onBackToResults)

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp),
                    text = selectedGroupName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FeatureList(elementListWithSelectedGroupName, onFeatureSelected)
        }
    }
}

@Composable
private fun FeatureList(assetTypeList: List<UtilityElement>, onFeatureSelected: (UtilityElement) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column {
            ExpandableCardWithLabel(assetTypeList[0].assetType.name, value = assetTypeList.size.toString()) {
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
