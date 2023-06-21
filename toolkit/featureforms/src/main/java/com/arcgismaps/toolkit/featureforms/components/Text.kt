package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput

@Composable
internal fun TextArea(
    field: FieldFeatureFormElement,
    modifier: Modifier = Modifier
) {
    val textBoxInput = field.inputType as TextAreaFeatureFormInput
    FormTextField(
        label = field.label,
        description = field.description,
        minLength = textBoxInput.minLength.toInt(),
        maxLength = textBoxInput.maxLength.toInt(),
        singleLine = false,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
internal fun TextBox(
    field: FieldFeatureFormElement,
    modifier: Modifier = Modifier
) {
    val textBoxInput = field.inputType as TextBoxFeatureFormInput
    FormTextField(
        label = field.label,
        description = field.description,
        minLength = textBoxInput.minLength.toInt(),
        maxLength = textBoxInput.maxLength.toInt(),
        singleLine = true,
        modifier = modifier.fillMaxSize()
    )
}
