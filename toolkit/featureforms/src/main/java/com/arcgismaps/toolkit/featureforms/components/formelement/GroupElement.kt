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

package com.arcgismaps.toolkit.featureforms.components.formelement

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.BaseGroupState
import kotlinx.coroutines.launch

@Composable
internal fun GroupElement(
    groupElement: GroupFormElement,
    state: BaseGroupState,
    modifier: Modifier = Modifier,
    colors: GroupElementColors = GroupElementDefaults.colors(),
    onDialogRequest: (BaseFieldState<*>, Int) -> Unit
) {
    val visible by groupElement.isVisible.collectAsState()
    if (visible) {
        GroupElement(
            label = state.label,
            description = state.description,
            expanded = state.expanded.value,
            fieldStates = state.fieldStates,
            modifier = modifier,
            colors = colors,
            onClick = {
                state.setExpanded(!state.expanded.value)
            },
            onDialogRequest = onDialogRequest
        )
    }
}

@Composable
private fun GroupElement(
    label: String,
    description: String,
    expanded: Boolean,
    fieldStates: Map<Int, BaseFieldState<*>>,
    modifier: Modifier = Modifier,
    colors: GroupElementColors,
    onClick: () -> Unit,
    onDialogRequest: ((BaseFieldState<*>, Int) -> Unit)? = null
) {
    var visibleChildren by rememberSaveable {
        // all are visible initially
        mutableStateOf(fieldStates.keys.toSet())
    }
    val isAnyChildVisible by remember {
        derivedStateOf { visibleChildren.isNotEmpty() }
    }
    Card(
        modifier = modifier,
        shape = GroupElementDefaults.containerShape,
        border = BorderStroke(GroupElementDefaults.borderThickness, colors.borderColor)
    ) {
        GroupElementHeader(
            modifier = Modifier.fillMaxWidth(),
            title = label,
            description = description,
            canExpand = isAnyChildVisible,
            isExpanded = expanded,
            onClick = onClick
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.background(colors.containerColor)
            ) {
                fieldStates.forEach { (key, state) ->
                    FieldElement(state = state) {
                        onDialogRequest?.invoke(state, key)
                    }
                }
            }
        }
    }
    LaunchedEffect(fieldStates) {
        fieldStates.forEach { entry ->
            launch {
                entry.value.isVisible.collect { visible ->
                    visibleChildren = if (visible) {
                        visibleChildren + entry.key
                    } else {
                        visibleChildren - entry.key
                    }
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
    canExpand: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(enabled = canExpand, onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (canExpand) {
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
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun GroupElementPreview() {
    GroupElement(
        label = "Title",
        description = "Description",
        expanded = false,
        fieldStates = mutableMapOf(),
        modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
        colors = GroupElementDefaults.colors(),
        onClick = {}
    )
}
