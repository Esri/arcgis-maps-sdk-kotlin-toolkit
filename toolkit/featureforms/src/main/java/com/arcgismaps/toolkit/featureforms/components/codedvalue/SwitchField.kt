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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField

@Composable
internal fun SwitchField(state: SwitchFieldState, modifier: Modifier = Modifier) {
    val valueCode by state.value.collectAsState()
    val checkedState = valueCode == state.onValue.code!!.toString()
    val value = if (checkedState) state.onValue.name else state.offValue.name
    val isEditable by state.isEditable.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    BaseTextField(
        text = value,
        onValueChange = {
            state.onValueChanged(it)
        },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = state.label,
        placeholder = state.placeholder,
        singleLine = true,
        suffix = {
            Switch(
                checked = checkedState,
                onCheckedChange = { newState ->
                    val newValue = (
                        if (newState)
                            state.onValue.code?.toString()
                        else
                            state.offValue.code?.toString()
                        ) ?: throw IllegalStateException("coded value code must not be null")
                    state.onValueChanged(newValue)
                },
                modifier = Modifier.semantics { contentDescription = "switch" },
                enabled = isEditable
            )
        },
        supportingText = {
            Text(
                text = state.description,
                modifier = Modifier.semantics { contentDescription = "description" },
            )
        },
        interactionSource = interactionSource
    )
    
    LaunchedEffect(valueCode) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                val newValue = (
                    if (checkedState)
                        state.offValue.code?.toString()
                    else
                        state.onValue.code?.toString()
                    ) ?: throw IllegalStateException("coded value code must not be null")
                state.onValueChanged(newValue)
            }
        }
    }
}
