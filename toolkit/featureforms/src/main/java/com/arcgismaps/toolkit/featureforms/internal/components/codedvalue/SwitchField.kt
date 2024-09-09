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

package com.arcgismaps.toolkit.featureforms.internal.components.codedvalue

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField

@Composable
internal fun SwitchField(state: SwitchFieldState, modifier: Modifier = Modifier) {
    val codeValue by state.value
    val checkedState = codeValue.data == state.onValue.code
    val value = if (checkedState) state.onValue.name else state.offValue.name
    val isEditable by state.isEditable.collectAsState()
    val interactionSource = remember(state) { MutableInteractionSource() }
    BaseTextField(
        text = value,
        onValueChange = {
            // nothing happens here. state.onValueChange is triggered by the switch control
        },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = state.label,
        placeholder = state.placeholder,
        supportingText = state.description,
        isError = false,
        isRequired = false,
        singleLine = true,
        interactionSource = interactionSource,
        hasValueExpression = state.hasValueExpression
    ) {
        Switch(
            checked = checkedState,
            onCheckedChange = { newState ->
                val code = if (newState) {
                    state.onValue.code
                } else {
                    state.offValue.code
                }
                state.onValueChanged(code)
            },
            modifier = Modifier
                .semantics { contentDescription = "switch" }
                .padding(horizontal = 10.dp),
            enabled = isEditable
        )
    }

    LaunchedEffect(codeValue) {
        interactionSource.interactions.collect {
            if (isEditable) {
                if (it is PressInteraction.Release) {
                    val code = if (checkedState) {
                        state.offValue.code
                    } else {
                        state.onValue.code
                    }
                    state.onValueChanged(code)
                }
            }
        }
    }
}
