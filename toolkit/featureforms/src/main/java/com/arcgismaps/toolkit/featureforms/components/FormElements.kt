package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.toolkit.featureforms.api.DateTimePickerFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.GroupFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import java.time.Instant

@Composable
internal fun FieldElement(field: FieldFeatureFormElement, formDefinition: FeatureFormDefinition) {
    val context = LocalContext.current
    when (field.inputType) {
        is TextAreaFeatureFormInput -> {
            formDefinition.getElementValue(field)?.let {
                field.value = it.toString()
            }
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    formDefinition = formDefinition,
                    context = context
                )
            )
        }
        
        is TextBoxFeatureFormInput -> {
            formDefinition.getElementValue(field)?.let {
                field.value = it.toString()
            }
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    formDefinition = formDefinition,
                    context = context
                )
            )
        }
        
        is DateTimePickerFeatureFormInput -> {
            formDefinition.getElementValue(field)?.let {
                if (it is Instant) {
                    val asLong = it.toEpochMilli()
                    field.value = asLong.toString()
                } else if (it is Long) {
                    field.value = it.toString()
                }
            }
            DateTimeField(state = DateTimeFieldState(featureFormElement = field, formDefinition = formDefinition))
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
