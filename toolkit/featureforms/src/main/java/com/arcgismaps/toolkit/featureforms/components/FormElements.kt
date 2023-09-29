package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxField
import com.arcgismaps.toolkit.featureforms.components.combo.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.utils.editValue

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm) {
    val context = LocalContext.current
    val visible by field.isVisible.collectAsState()
    val scope = rememberCoroutineScope()

    if (visible) {
        when (field.input) {
            is TextAreaFormInput -> {
                val input = field.input as TextAreaFormInput
                val state = rememberSaveable(
                    form, saver =
                    FormTextFieldState.Saver(field, form, context, scope)
                ) {
                    FormTextFieldState(
                        formElement = field,
                        scope = scope,
                        onEditValue = { form.editValue(field, it) },
                        onEvaluateExpression = { form.evaluateExpressions() },
                        singleLine = false,
                        minLength = input.minLength.toInt(),
                        maxLength = input.maxLength.toInt(),
                        context = context
                    )
                }
                FormTextField(state = state)
            }

            is TextBoxFormInput -> {
                val input = field.input as TextBoxFormInput
                val state = rememberSaveable(
                    form, saver =
                    FormTextFieldState.Saver(field, form, context, scope)
                ) {
                    FormTextFieldState(
                        formElement = field,
                        scope = scope,
                        onEditValue = { form.editValue(field, it) },
                        onEvaluateExpression = { form.evaluateExpressions() },
                        singleLine = false,
                        minLength = input.minLength.toInt(),
                        maxLength = input.maxLength.toInt(),
                        context = context
                    )
                }
                FormTextField(state = state)
            }

            is DateTimePickerFormInput -> {
                DateTimeField(
                    state = DateTimeFieldState(
                        formElement = field,
                        form = form,
                        scope = scope
                    )
                )
            }

            is ComboBoxFormInput -> {
                ComboBoxField(
                    state = ComboBoxFieldState(
                        formElement = field,
                        featureForm = form,
                        context = context,
                        scope = scope
                    )
                )
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
