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

import android.util.Log
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.formelement.RecompositionScope
import com.arcgismaps.toolkit.featureforms.utils.isNullOrEmptyString

@Composable
internal fun RadioButtonField(
    state: RadioButtonFieldState,
    modifier: Modifier = Modifier,
    colors: RadioButtonFieldColors = RadioButtonFieldDefaults.colors()
) {
    val editable by state.isEditable.collectAsState()
    val required by state.isRequired.collectAsState()
    val noValueLabel = state.noValueLabel.ifEmpty { stringResource(R.string.no_value) }
    RadioButtonField(
        label = state.label,
        description = state.description,
        valueProvider = { state.value.value.data },
        editable = editable,
        required = required,
        codedValues = state.codedValues.associateBy({ it.code }, { it.name }),
        showNoValueOption = state.showNoValueOption,
        noValueLabel = noValueLabel,
        modifier = modifier,
        colors = colors
    ) {
        state.onValueChanged(it)
    }
}

@Composable
private fun RadioButtonField(
    label: String,
    description: String,
    valueProvider: () -> Any?,
    editable: Boolean,
    required: Boolean,
    codedValues: Map<Any?, String>,
    showNoValueOption: FormInputNoValueOption,
    noValueLabel: String,
    modifier: Modifier = Modifier,
    colors: RadioButtonFieldColors = RadioButtonFieldDefaults.colors(),
    onValueChanged: (Any?) -> Unit = {}
) {
    val value = valueProvider()
    val options = if (!required) {
        if (showNoValueOption == FormInputNoValueOption.Show) {
            mapOf(null to noValueLabel) + codedValues
        } else codedValues
    } else codedValues

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (required) {
                "$label *"
            } else {
                label
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelColor
        )
        Column(
            modifier = Modifier
                .selectableGroup()
                .border(
                    width = 1.dp,
                    color = colors.containerBorderColor,
                    shape = RoundedCornerShape(5.dp)
                )
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textColor
            ) {
                options.forEach { (code, name) ->
                    RadioButtonRow(
                        value = name,
                        selected = code == value || (name == noValueLabel && value == null),
                        enabled = editable,
                        onClick = { onValueChanged(code) }
                    )
                }
            }
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.supportingTextColor
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, showSystemUi = true)
@Composable
private fun RadioButtonFieldPreview() {
    MaterialTheme {
        RadioButtonField(
            label = "A list of values",
            description = "Description",
            valueProvider = { "" },
            editable = true,
            required = true,
            codedValues = mapOf(
                "One" to "One",
                "Two" to "Two",
                "Three" to "Three"
            ),
            showNoValueOption = FormInputNoValueOption.Show,
            noValueLabel = "No Value",
        ) { }
    }
}
