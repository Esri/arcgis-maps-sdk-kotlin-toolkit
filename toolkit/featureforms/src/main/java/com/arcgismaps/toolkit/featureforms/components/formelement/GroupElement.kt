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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.BaseGroupState

@Composable
internal fun GroupElement(
    groupElement: GroupFormElement,
    state: BaseGroupState,
    modifier: Modifier = Modifier,
    onDialogRequest: (BaseFieldState, Int) -> Unit
) {
    val visible by groupElement.isVisible.collectAsState()
    if (visible) {
        GroupElement(
            label = state.label,
            description = state.description,
            expanded = state.expanded.value,
            fieldStates = state.fieldStates,
            modifier = modifier,
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
    fieldStates: Map<Int, BaseFieldState?>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDialogRequest: ((BaseFieldState, Int) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        GroupElementHeader(
            modifier = Modifier.fillMaxWidth(),
            title = label,
            description = description,
            isExpanded = expanded,
            onClick = onClick
        )
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                fieldStates.forEach { (key, state) ->
                    if (state != null) {
                        FieldElement(state = state) {
                            onDialogRequest?.invoke(state, key)
                        }
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
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = modifier
        .clickable {
            onClick()
        }
        .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
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
    GroupElement(
        label = "Title",
        description = "Description",
        expanded = false,
        fieldStates = mutableMapOf(),
        onClick = {}
    )
}
