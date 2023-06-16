package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput

@Composable
public fun TextBox(
    field: FieldFeatureFormElement,
    modifier: Modifier = Modifier
) {
    val textBoxInput = field.inputType as TextBoxFeatureFormInput
    FormTextField(
        label = field.label,
        modifier = modifier,
        description = field.description,
        minLength = textBoxInput.minLength.toInt(),
        maxLength = textBoxInput.maxLength.toInt()
    )
}
