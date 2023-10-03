package com.arcgismaps.toolkit.featureforms.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxField
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldProperties
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.launch

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm, state: BaseFieldState?) {
    val visible by field.isVisible.collectAsState()
    val scope = rememberCoroutineScope()
    if (visible) {
        when (field.input) {
            is TextBoxFormInput, is TextAreaFormInput -> {
                state?.let {
                    FormTextField(state = state as FormTextFieldState)
                }
            }

            is DateTimePickerFormInput -> {
                val input = field.input as DateTimePickerFormInput
                DateTimeField(
                    state = DateTimeFieldState(
                        properties = DateTimeFieldProperties(
                            label = field.label,
                            placeholder = field.hint,
                            description = field.description,
                            value = field.value,
                            editable = field.isEditable,
                            required = field.isRequired,
                            minEpochMillis = input.min?.toEpochMilli(),
                            maxEpochMillis = input.max?.toEpochMilli(),
                            shouldShowTime = input.includeTime
                        ),
                        scope = scope,
                        onEditValue = {
                            form.editValue(field, it)
                            scope.launch { form.evaluateExpressions() }
                        }
                    )
                )
            }

            is ComboBoxFormInput -> {
                state?.let {
                    ComboBoxField(state = state as ComboBoxFieldState)
                }
            }

            else -> { /* TO-DO: add support for other input types */
            }
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
internal fun GroupElement(group: GroupFormElement, form: FeatureForm) {
    // To-do
}
