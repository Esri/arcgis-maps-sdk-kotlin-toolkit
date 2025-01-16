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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

@Composable
internal fun UtilityNetworkAssociationsElement(
    state: UtilityNetworkAssociationsElementState,
    onAssociationTypeClick: () -> Unit,
    //onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val associations by state.associations
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
                associations.keys.toList(),
                {
                    state.selectedAssociationType = it
                    onAssociationTypeClick()
                },
                Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
internal fun UtilityNetworkAssociationType(
    state: UtilityNetworkAssociationsElementState,
    onBackPressed: () -> Unit,
    onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.selectedAssociationType == null) {
        // return to the previous screen if no association type is selected
        onBackPressed()
        return
    }
    val associations by state.associations
    Column(modifier = modifier) {
        AssociationTypeHeader(
            state.selectedAssociationType!!.name,
            state.label,
            onBackPressed = onBackPressed,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
        HorizontalDivider()
        Associations(
            associations = associations[state.selectedAssociationType] ?: emptyList(),
            displayCount = state.displayCount,
            onUtilityElementClick = onUtilityElementClick,
            modifier = Modifier.padding(16.dp)
        )
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
private fun AssociationTypeHeader(
    label: String,
    source: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
                .size(32.dp)
                .clickable {
                    onBackPressed()
                }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = source,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun AssociationTypes(
    types: List<UtilityAssociationType>,
    onClick: (UtilityAssociationType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        types.forEachIndexed { i, type ->
            ListItem(
                headlineContent = {
                    Text(text = type.name)
                },
                modifier = Modifier.clickable {
                    onClick(type)
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Random(System.currentTimeMillis()).nextInt(0, 100).toString()
                        )
                        Image(
                            imageVector = Icons.AutoMirrored.Default.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            )
            if (i < types.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Associations(
    associations: List<UtilityAssociation>,
    displayCount: Int,
    onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val map = associations
        .asSequence()
        .map { it.toElement }
        .groupBy { it.networkSource }
    LazyColumn(
        modifier = modifier
    ) {
        map.keys.forEach {
            item {
                var expanded by rememberSaveable {
                    mutableStateOf(false)
                }
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded = !expanded
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = map[it]?.count().toString(),
                        )
                    }
                    if (expanded) {
                        NetworkSourceContent(
                            elements = map[it] ?: emptyList(),
                            displayCount = displayCount,
                            onUtilityElementClick = onUtilityElementClick,
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun NetworkSourceContent(
    elements: List<UtilityElement>,
    displayCount: Int,
    onUtilityElementClick: (UtilityElement) -> Unit,
    modifier: Modifier = Modifier
) {
    // show the list of utility elements
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        elements.forEachIndexed { index, element ->
            Column {
                ListItem(
                    headlineContent = {
                        Text(text = element.objectId.toString())
                    },
                    modifier = Modifier.clickable {
                        onUtilityElementClick(element)
                    }
                )
                if (index < elements.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

private val UtilityAssociationType.name : String
    get() {
        val input = this.toString()
        val regex = Regex("UtilityAssociationType\\$(\\w+)@")
        val matchResult = regex.find(input)
        return matchResult?.groupValues?.get(1) ?: input
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

@Preview(showBackground = true)
@Composable
private fun AssociationTypeHeaderPreview() {
    AssociationTypeHeader("Fuse Box", "Feature", {}, Modifier.fillMaxWidth())
}
