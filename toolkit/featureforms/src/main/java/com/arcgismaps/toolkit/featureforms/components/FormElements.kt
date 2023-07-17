package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.GroupFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(field: FieldFeatureFormElement) {
    val context = LocalContext.current
    when (field.inputType) {
        is TextAreaFeatureFormInput  -> {
            FormTextField(state = FormTextFieldState(field, context))
        }
        is TextBoxFeatureFormInput -> {
            FormTextField(state = FormTextFieldState(field, context))
        }
        else -> { /* TO-DO: add support for other input types */}
    }
}

@Composable
internal fun GroupElement(group: GroupFeatureFormElement) {
    // To-do
}
