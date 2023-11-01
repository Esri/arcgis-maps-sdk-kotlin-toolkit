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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

internal class DateTimeFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<Instant?>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    val minEpochMillis: Instant?,
    val maxEpochMillis: Instant?,
    val shouldShowTime: Boolean
) : FieldProperties<Instant?>(label, placeholder, description, value, required, editable, visible)

/**
 * A class to handle the state of a [DateTimeField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [DateTimeFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [FormTextFieldState.onValueChanged].
 */
internal class DateTimeFieldState(
    properties: DateTimeFieldProperties,
    initialValue: Instant? = properties.value.value,
    scope: CoroutineScope,
    onEditValue: (Any?) -> Unit
) : BaseFieldState<Instant?>(
    properties = properties,
    initialValue = initialValue,
    scope = scope,
    onEditValue = onEditValue
) {
    val minEpochMillis: Instant? = properties.minEpochMillis

    val maxEpochMillis: Instant? = properties.maxEpochMillis

    val shouldShowTime: Boolean = properties.shouldShowTime
    
    companion object {
        internal fun dateTimeValue(value: StateFlow<Any?>, scope: CoroutineScope): StateFlow<Instant?> =
            value.map {
                if (it is Instant?) {
                    it
                } else {
                    throw IllegalArgumentException("expected a date time for the value of a date time field")
                }
            }.stateIn(scope, SharingStarted.Eagerly, value.value as Instant?)
        
        fun Saver(
            field: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<DateTimeFieldState, Any> = listSaver(
            save = {
                listOf(it.value.value)
            },
            restore = { list ->
                val input = field.input as DateTimePickerFormInput
                DateTimeFieldState(
                    properties = DateTimeFieldProperties(
                        label = field.label,
                        placeholder = field.hint,
                        description = field.description,
                        value = dateTimeValue(field.value, scope),
                        editable = field.isEditable,
                        required = field.isRequired,
                        visible = field.isVisible,
                        minEpochMillis = input.min,
                        maxEpochMillis = input.max,
                        shouldShowTime = input.includeTime
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = {
                        form.editValue(field, it)
                        scope.launch { form.evaluateExpressions() }
                    }
                )
            }
        )
    }
}

@Composable
internal fun rememberDateTimeFieldState(
    field: FieldFormElement,
    minEpochMillis: Instant?,
    maxEpochMillis: Instant?,
    shouldShowTime: Boolean,
    form: FeatureForm,
    scope: CoroutineScope
): DateTimeFieldState = rememberSaveable(
    saver = DateTimeFieldState.Saver(
        field = field,
        form = form,
        scope = scope
    )
) {
    DateTimeFieldState(
        properties = DateTimeFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = DateTimeFieldState.dateTimeValue(field.value, scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            minEpochMillis = minEpochMillis,
            maxEpochMillis = maxEpochMillis,
            shouldShowTime = shouldShowTime
        ),
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}

