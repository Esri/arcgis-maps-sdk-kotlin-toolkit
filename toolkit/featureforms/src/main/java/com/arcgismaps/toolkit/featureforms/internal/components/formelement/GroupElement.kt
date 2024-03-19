/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.formelement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.theme.LocalFeatureFormTheme
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.MutableFormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme

@Composable
internal fun GroupElement(
    state: BaseGroupState,
    modifier: Modifier = Modifier
) {
    val visible by state.isVisible.collectAsState()
    if (visible) {
        GroupElement(
            label = state.label,
            description = state.description,
            expanded = state.expanded.value,
            fieldStates = state.fieldStates,
            modifier = modifier,
            onClick = {
                state.setExpanded(!state.expanded.value)
            }
        )
    }
}

@Composable
private fun GroupElement(
    label: String,
    description: String,
    expanded: Boolean,
    fieldStates: FormStateCollection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalFeatureFormTheme.current.colorScheme.groupElementColors
    Card(
        modifier = modifier,
        shape = GroupElementDefaults.containerShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.headerColor
        ),
        border = BorderStroke(GroupElementDefaults.borderThickness, colors.outlineColor)
    ) {
        GroupElementHeader(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    toggleableState = if (expanded) ToggleableState.On else ToggleableState.Off
                },
            title = label,
            description = description,
            isExpanded = expanded,
            onClick = onClick
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.background(colors.containerColor)
            ) {
                fieldStates.forEach {
                    FieldElement(state = it.getState<BaseFieldState<*>>())
                }
            }
        }
    }
}

@Composable
private fun GroupElementHeader(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalFeatureFormTheme.current
    val colors = theme.colorScheme.groupElementColors
    val typography = theme.typography.groupElementTypography
    Row(modifier = modifier
        .clickable {
            onClick()
        }
        .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.labelColor,
                style = typography.labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = colors.supportingTextColor,
                    style = typography.supportingTextStyle
                )
            }
        }
        Crossfade(targetState = isExpanded, label = "expanded-icon-anim") {
            Icon(
                imageVector = if (it) {
                    Icons.Rounded.ExpandLess
                } else {
                    Icons.Rounded.ExpandMore
                },
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun GroupElementPreview() {
    FeatureFormTheme {
        GroupElement(
            label = "Title",
            description = "Description",
            expanded = false,
            fieldStates = MutableFormStateCollection(),
            modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
            onClick = {}
        )
    }
}
