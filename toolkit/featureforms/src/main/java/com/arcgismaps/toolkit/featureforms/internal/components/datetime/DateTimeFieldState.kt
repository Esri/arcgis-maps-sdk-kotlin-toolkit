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

package com.arcgismaps.toolkit.featureforms.internal.components.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFieldProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

internal class DateTimeFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<Instant?>,
    validationErrors: StateFlow<List<ValidationErrorState>>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    fieldType: FieldType,
    val minEpochMillis: Instant?,
    val maxEpochMillis: Instant?,
    val shouldShowTime: Boolean
) : FieldProperties<Instant?>(
    label,
    placeholder,
    description,
    value,
    validationErrors,
    required,
    editable,
    visible,
    fieldType,
    null
)

/**
 * A class to handle the state of a [DateTimeField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [TextFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [DateTimeFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param updateValue a function that is invoked when the user edits result in a change of value. This
 * is called in [BaseFieldState.onValueChanged].
 * @param evaluateExpressions a function that is invoked to evaluate all form expressions. This is
 * called after a successful [updateValue].
 */
internal class DateTimeFieldState(
    id : Int,
    properties: DateTimeFieldProperties,
    initialValue: Instant? = properties.value.value,
    hasValueExpression : Boolean,
    scope: CoroutineScope,
    updateValue: (Any?) -> Unit,
    evaluateExpressions: suspend () -> Result<List<FormExpressionEvaluationError>>
) : BaseFieldState<Instant?>(
    id = id,
    properties = properties,
    initialValue = initialValue,
    hasValueExpression = hasValueExpression,
    scope = scope,
    updateValue = updateValue,
    evaluateExpressions = evaluateExpressions
) {
    val minEpochMillis: Instant? = properties.minEpochMillis

    val maxEpochMillis: Instant? = properties.maxEpochMillis

    val shouldShowTime: Boolean = properties.shouldShowTime

    override fun typeConverter(input: Instant?): Any? = input
    
    companion object {
        fun Saver(
            field: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<DateTimeFieldState, Any> = listSaver(
            save = {
                listOf(it.value.value.data, it.wasFocused)
            },
            restore = { list ->
                val input = field.input as DateTimePickerFormInput
                DateTimeFieldState(
                    id = field.hashCode(),
                    properties = DateTimeFieldProperties(
                        label = field.label,
                        placeholder = field.hint,
                        description = field.description,
                        value = field.mapValueAsStateFlow(scope),
                        validationErrors = field.mapValidationErrors(scope),
                        editable = field.isEditable,
                        required = field.isRequired,
                        visible = field.isVisible,
                        minEpochMillis = input.min,
                        maxEpochMillis = input.max,
                        shouldShowTime = input.includeTime,
                        fieldType = field.fieldType
                    ),
                    initialValue = list[0] as Instant?,
                    hasValueExpression = field.hasValueExpression,
                    scope = scope,
                    updateValue = field::updateValue,
                    evaluateExpressions = form::evaluateExpressions
                ).apply {
                    onFocusChanged(list[1] as Boolean)
                }
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
    inputs = arrayOf(form),
    saver = DateTimeFieldState.Saver(
        field = field,
        form = form,
        scope = scope
    )
) {
    DateTimeFieldState(
        id = field.hashCode(),
        properties = DateTimeFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.mapValueAsStateFlow(scope),
            validationErrors = field.mapValidationErrors(scope),
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            minEpochMillis = minEpochMillis,
            maxEpochMillis = maxEpochMillis,
            shouldShowTime = shouldShowTime,
            fieldType = field.fieldType
        ),
        hasValueExpression = field.hasValueExpression,
        scope = scope,
        updateValue = field::updateValue,
        evaluateExpressions = form::evaluateExpressions
    )
}
