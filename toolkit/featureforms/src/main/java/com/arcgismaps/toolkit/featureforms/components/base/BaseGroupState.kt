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

package com.arcgismaps.toolkit.featureforms.components.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FormGroupState
import com.arcgismaps.mapping.featureforms.GroupFormElement

internal class GroupProperties(
    val label: String,
    val description: String,
    val expanded: Boolean
)

public class BaseGroupState internal constructor(
    properties: GroupProperties,
    public val fieldStates: Map<Int, BaseFieldState<*>>
) {
    public val label : String = properties.label

    public val description : String = properties.description

    private val _expanded = mutableStateOf(properties.expanded)
    public val expanded : State<Boolean> = _expanded

    public fun setExpanded(value : Boolean) {
        _expanded.value = value
    }

    public companion object {
        public fun Saver(fieldStates: Map<Int, BaseFieldState<*>>): Saver<BaseGroupState, Any> = listSaver(
            save = {
                listOf(it.label, it.description, it.expanded.value)
            },
            restore = {
                val properties = GroupProperties(
                    label = it[0] as String,
                    description = it[1] as String,
                    expanded = it[2] as Boolean
                )
                BaseGroupState(
                    properties = properties,
                    fieldStates = fieldStates
                )
            }
        )
    }
}

@Composable
internal fun rememberBaseGroupState(
    groupElement: GroupFormElement,
    fieldStates: Map<Int, BaseFieldState<*>>
): BaseGroupState = rememberSaveable(
    saver = BaseGroupState.Saver(fieldStates)
) {
    BaseGroupState(
        properties = GroupProperties(
            label = groupElement.label,
            description = groupElement.description,
            expanded = groupElement.initialState == FormGroupState.Expanded
        ),
        fieldStates = fieldStates
    )
}
