/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.featureforms.components.datetime

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

internal interface DateTimePickerState {
    val minDateTime: Long?
    val maxDateTime: Long?
    val value: Long?
    val pickerStyle: DateTimePickerStyle
    val label: String
    val description: String
    val visible: State<Boolean>
    val onValueSet: (Long) -> Unit
    fun setVisibility(visible: Boolean)
}

private class DateTimePickerStateImpl(
    override val pickerStyle: DateTimePickerStyle,
    override val minDateTime: Long?,
    override val maxDateTime: Long?,
    override val value: Long?,
    override val label: String,
    override val description: String,
    override val onValueSet: (Long) -> Unit
) : DateTimePickerState {
    override var visible = mutableStateOf(false)
        private set
    
    override fun setVisibility(visible: Boolean) {
        this.visible.value = visible
    }
    
}

internal fun DateTimePickerState(
    type: DateTimePickerStyle,
    minDateTime: Long?,
    maxDateTime: Long?,
    value: Long?,
    label: String,
    description: String,
    onValueSet: (Long) -> Unit
): DateTimePickerState =
    DateTimePickerStateImpl(
        type,
        minDateTime,
        maxDateTime,
        value,
        label,
        description,
        onValueSet
    )
