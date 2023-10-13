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

package com.arcgismaps.toolkit.featureforms.components.codedvalue

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.CodedValue
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun RadioButtonField(
    state: RadioButtonFieldState,
    modifier: Modifier = Modifier,
    colors: RadioButtonFieldColors = RadioButtonFieldDefaults.colors()
) {
    val value by state.value.collectAsState()
    val editable by state.isEditable.collectAsState()
    val required by state.isRequired.collectAsState()
    val noValueLabel = state.noValueLabel.ifEmpty { stringResource(R.string.no_value) }
    val options = if (!required) {
        if (state.showNoValueOption == FormInputNoValueOption.Show) {
            mapOf("" to noValueLabel) + state.codedValues.associateBy({ it.code }, { it.name })
        } else state.codedValues.associateBy({ it.code }, { it.name })
    } else state.codedValues.associateBy({ it.code }, { it.name })

    val label = remember(required) {
        if (required) {
            "${state.label} *"
        } else {
            state.label
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelColor(enabled = editable)
        )
        Column(
            modifier = Modifier
                .selectableGroup()
                .border(
                    width = 1.dp,
                    color = colors.containerBorderColor(enabled = editable),
                    shape = RoundedCornerShape(5.dp)
                )
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textColor(enabled = editable)
            ) {
                options.forEach { (code, name) ->
                    RadioButtonRow(
                        value = name,
                        selected = (code?.toString()
                            ?: "") == value || (name == noValueLabel && value.isEmpty()),
                        enabled = editable,
                        onClick = { state.onValueChanged(code?.toString() ?: "") }
                    )
                }
            }
        }
        if (state.description.isNotEmpty()) {
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.supportingTextColor(enabled = editable)
            )
        }
    }

}

@Composable
private fun RadioButtonRow(
    value: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


//@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, showSystemUi = true)
//@Composable
//private fun RadioButtonFieldPreview() {
//    MaterialTheme {
//        val state = RadioButtonFieldState(
//            properties = RadioButtonFieldProperties(
//                label = "A list of values",
//                placeholder = "Placeholder",
//                description = "Description",
//                value = MutableStateFlow(""),
//                editable = MutableStateFlow(true),
//                required = MutableStateFlow(false),
//                codedValues = listOf("One", "Two", "Three"),
//                showNoValueOption = FormInputNoValueOption.Show,
//                noValueLabel = "No Value"
//            ),
//            scope = CoroutineScope(Dispatchers.IO),
//            onEditValue = {}
//        )
//        RadioButtonField(state = state)
//    }
//}
