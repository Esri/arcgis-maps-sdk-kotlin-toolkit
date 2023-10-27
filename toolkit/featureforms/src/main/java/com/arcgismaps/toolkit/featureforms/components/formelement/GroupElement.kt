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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormGroupState
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.id
import com.arcgismaps.toolkit.featureforms.rememberFieldStates

@Composable
internal fun GroupElement(
    groupElement: GroupFormElement,
    state: BaseGroupState,
    modifier: Modifier = Modifier
) {
    val visible by groupElement.isVisible.collectAsState()
    if (visible) {
        GroupElement(
            label = state.label,
            description = state.description,
            expanded = state.expanded,
            modifier = modifier
        )
    }
}

@Composable
private fun GroupElement(
    label: String,
    description: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    var contextExpanded by remember {
        mutableStateOf(expanded)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(5.dp)
            ),
    ) {
        GroupElementHeader(
            modifier = Modifier.fillMaxWidth(),
            title = label,
            description = description,
            isExpanded = contextExpanded
        ) {
            contextExpanded = !contextExpanded
        }
        GroupElementContent(isVisible = contextExpanded)
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
        if (isExpanded) {
            Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = null)
        } else {
            Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@Composable
private fun GroupElementContent(
    isVisible: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (isVisible) {
        Column(
            modifier = Modifier
                .background(color = Color.LightGray)
                .fillMaxSize()
        ) {
            Text(text = "Hello")
//            formElements.forEach { formElement ->
//                if (formElement is FieldFormElement) {
//                    states[formElement.id]?.let { state ->
//                        FieldElement(field = formElement, state = state)
//                    }
//                }
//            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF, showSystemUi = true)
@Composable
private fun GroupElementPreview() {
    GroupElement(
        label = "Title",
        description = "Description",
        expanded = false
    )
}
