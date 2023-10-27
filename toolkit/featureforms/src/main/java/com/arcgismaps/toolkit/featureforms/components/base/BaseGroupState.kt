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

internal class BaseGroupState(
    properties: GroupProperties,
    val fieldStates: Map<Int, BaseFieldState?>
) {
    val label = properties.label

    val description = properties.description

    val expanded = properties.expanded

    companion object {
        fun Saver(fieldStates: Map<Int, BaseFieldState?>): Saver<BaseGroupState, Any> = listSaver(
            save = {
                listOf(it.label, it.description, it.expanded)
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
    group: GroupFormElement,
    fieldStates: Map<Int, BaseFieldState?>
): BaseGroupState = rememberSaveable(
    saver = BaseGroupState.Saver(fieldStates)
) {
    BaseGroupState(
        properties = GroupProperties(
            label = group.label,
            description = group.description,
            expanded = group.initialState == FormGroupState.Expanded
        ),
        fieldStates = fieldStates
    )
}
