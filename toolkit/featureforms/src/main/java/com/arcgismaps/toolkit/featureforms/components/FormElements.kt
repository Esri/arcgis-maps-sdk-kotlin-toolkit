package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.GroupFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput

@Composable
internal fun FieldElement(field: FieldFeatureFormElement) {
    when (field.inputType) {
        is TextAreaFeatureFormInput  -> {
            // TextArea(field = field)
            FormTextField(formTextFieldState = FormTextFieldState(field), field.inputType)
        }
        is TextBoxFeatureFormInput -> {
            // TextBox(field = field)
            FormTextField(formTextFieldState = FormTextFieldState(field), field.inputType)
        }
        else -> { /* TO-DO: add support for other input types */}
    }
}

@Composable
internal fun GroupElement(group: GroupFeatureFormElement) {
    Text("TO-DO")
}
