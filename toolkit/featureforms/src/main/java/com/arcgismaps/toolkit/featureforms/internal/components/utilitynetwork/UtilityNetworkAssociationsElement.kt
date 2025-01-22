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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetworkSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

@Composable
internal fun UtilityNetworkAssociationsElement(
    state: UtilityNetworkAssociationsElementState,
    onAssociationTypeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups by state.groups
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            ElementHeader(
                state.label,
                state.description,
                Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            AssociationTypes(
                groups = groups,
                onClick = {
                    onAssociationTypeClick(it)
                },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ElementHeader(
    label: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = Icons.Default.DatasetLinked,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun AssociationTypes(
    groups: List<UtilityAssociationGroup>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        groups.forEachIndexed { i, group ->
            ListItem(
                headlineContent = {
                    Text(text = group.type.name)
                },
                modifier = Modifier.clickable {
                    onClick(i)
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.count.toString(),
                        )
                        Image(
                            imageVector = Icons.AutoMirrored.Default.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            )
            if (i < groups.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UtilityNetworkAssociationsElementPreview() {
    val state = UtilityNetworkAssociationsElementState(
        id = 0,
        label = "Associations",
        description = "This is a description",
        isVisible = MutableStateFlow(true),
        utilityNetwork = null,
        utilityElement = null,
        scope = rememberCoroutineScope()
    )
    UtilityNetworkAssociationsElement(state = state, {})
}

@Preview(showBackground = true)
@Composable
private fun ElementHeaderPreview() {
    ElementHeader("Associations", "This is a description", Modifier.fillMaxWidth())
}
