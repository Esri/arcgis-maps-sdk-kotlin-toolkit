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
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FormGroupState
import com.arcgismaps.mapping.featureforms.GroupFormElement
import kotlinx.coroutines.flow.StateFlow

internal class BaseGroupState(
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    expanded: Boolean,
    val fieldStates: FormStateCollection
) : FormElementState(
    label = label,
    description = description,
    isVisible = isVisible
) {
    private val _expanded = mutableStateOf(expanded)
    val expanded: State<Boolean> = _expanded

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    companion object {
        fun Saver(
            groupElement: GroupFormElement,
            fieldStates: FormStateCollection
        ): Saver<BaseGroupState, Boolean> = Saver(
            save = {
                it.expanded.value
            },
            restore = {
                BaseGroupState(
                    label = groupElement.label,
                    description = groupElement.description,
                    isVisible = groupElement.isVisible,
                    expanded = it,
                    fieldStates = fieldStates
                )
            }
        )
    }
}

@Composable
internal fun rememberBaseGroupState(
    groupElement: GroupFormElement,
    fieldStates: FormStateCollection
): BaseGroupState = rememberSaveable(
    saver = BaseGroupState.Saver(groupElement, fieldStates)
) {
    BaseGroupState(
        label = groupElement.label,
        description = groupElement.description,
        isVisible = groupElement.isVisible,
        expanded = groupElement.initialState == FormGroupState.Expanded,
        fieldStates = fieldStates
    )
}
