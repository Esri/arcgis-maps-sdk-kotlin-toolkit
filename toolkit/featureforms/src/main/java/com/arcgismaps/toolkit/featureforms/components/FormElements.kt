package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.ComboBoxField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.RadioButtonField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.RadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.SwitchField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.SwitchFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(field: FieldFormElement, state: BaseFieldState) {
    val visible by field.isVisible.collectAsState()
    if (visible) {
        when (field.input) {
            is TextBoxFormInput, is TextAreaFormInput -> {
                FormTextField(state = state as FormTextFieldState)
            }

            is DateTimePickerFormInput -> {
                DateTimeField(state = state as DateTimeFieldState)
            }

            is ComboBoxFormInput -> {
                ComboBoxField(state = state as CodedValueFieldState)
            }
    
            is SwitchFormInput -> {
                val switchState = state as SwitchFieldState
                if (!switchState.fallback) {
                    SwitchField(state = state)
                } else {
                    ComboBoxField(state = state)
                }
            }

            is RadioButtonsFormInput -> {
                if ((state as RadioButtonFieldState).shouldFallback()) {
                    ComboBoxField(state = state)
                } else {
                    RadioButtonField(state = state)
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
