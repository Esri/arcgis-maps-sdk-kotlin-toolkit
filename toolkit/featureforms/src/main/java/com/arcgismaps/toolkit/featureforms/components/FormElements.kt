package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.GroupFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(field: FieldFeatureFormElement, formDefinition: FeatureFormDefinition) {
    val context = LocalContext.current
    when (field.inputType) {
        is TextAreaFeatureFormInput -> {
            formDefinition.getElementValue(field)?.let {
                field.value = it as String
            }
            FormTextField(state = FormTextFieldState(field, formDefinition, context))
        }
        
        is TextBoxFeatureFormInput -> {
            formDefinition.getElementValue(field)?.let {
                field.value = it as String
            }
            FormTextField(state = FormTextFieldState(field, formDefinition, context))
        }
        
        else -> { /* TO-DO: add support for other input types */
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
internal fun GroupElement(group: GroupFeatureFormElement, formDefinition: FeatureFormDefinition) {
    // To-do
}

/**
 * Retrieve the value of a [FieldFeatureFormElement] from the [FeatureFormDefinition].
 * This call is likely to be pushed into core.
 */
private fun FeatureFormDefinition.getElementValue(formElement: FieldFeatureFormElement): Any? {
    return feature?.attributes?.get(formElement.fieldName)
}

