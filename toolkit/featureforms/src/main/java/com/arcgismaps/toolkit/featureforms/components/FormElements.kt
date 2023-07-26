package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm) {
    val context = LocalContext.current
    when (field.input) {
        is TextAreaFormInput -> {
//            form.getElementValue(field)?.let {
//                field.value = it.toString()
//            }
            FormTextField(state = FormTextFieldState(
                featureFormElement = field,
                form = form,
                context = context))
        }
        
        is TextBoxFormInput -> {
//            formDefinition.getElementValue(field)?.let {
//                field.value = it.toString()
//            }
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    form = form,
                    context = context
                )
            )
        }
        
        else -> { /* TO-DO: add support for other input types */
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
internal fun GroupElement(group: GroupFormElement, formDefinition: FeatureFormDefinition) {
    // To-do
}

/**
 * Retrieve the value of a [FieldFormElement] from the [FeatureFormDefinition].
 * This call is likely to be pushed into core.
 */
@Suppress("unused")
private fun FeatureForm.getElementValue(formElement: FieldFormElement): Any? {
    return feature.attributes[formElement.fieldName]
}

