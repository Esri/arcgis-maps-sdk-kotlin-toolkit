package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
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

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm) {
    val context = LocalContext.current
    when (field.input) {
        is TextAreaFormInput -> {
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    form = form,
                    context = context
                )
            )
        }

        is TextBoxFormInput -> {
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    form = form,
                    context = context
                )
            )
        }

        is DateTimePickerFormInput -> {
            DateTimeField(
                state = DateTimeFieldState(
                    formElement = field,
                    form = form
                )
            )
        }

        is ComboBoxFormInput -> {
            ComboBoxField(
                state = ComboBoxFieldState(
                    field,
                    form
                )
            )
        }

        else -> { /* TO-DO: add support for other input types */
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
internal fun GroupElement(group: GroupFormElement, form: FeatureForm) {
    // To-do
}
