package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState
import java.time.Instant

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm) {
    val context = LocalContext.current
<<<<<<< HEAD
    when (field.input) {
        is TextAreaFormInput -> {
	    form.getElementValue(field)?.let {
                field.value = it.toString()
            }
            FormTextField(state = FormTextFieldState(
                featureFormElement = field,
                form = form,
                context = context))
        }
        
        is TextBoxFormInput -> {
	    form.getElementValue(field)?.let {
                field.value = it.toString()
            }	
            FormTextField(
                state = FormTextFieldState(
                    featureFormElement = field,
                    form = form,
                    context = context
                )
            )
        }

        is DateTimePickerFeatureFormInput -> {
            form.getElementValue(field)?.let {
                if (it is Instant) {
                    val asLong = it.toEpochMilli()
                    field.value = asLong.toString()
                } else if (it is Long) {
                    field.value = it.toString()
                }
            }
            DateTimeField(
                state = DateTimeFieldState(
                    featureFormElement = field,
                    form = form
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
