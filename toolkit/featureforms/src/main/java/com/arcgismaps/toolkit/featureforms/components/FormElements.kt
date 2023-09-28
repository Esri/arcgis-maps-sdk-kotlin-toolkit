package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(field: FieldFormElement, form: FeatureForm) {
    val context = LocalContext.current
    val visible by field.isVisible.collectAsState()
    val scope = rememberCoroutineScope()
    
    if (visible) {
        when (field.input) {
            is TextAreaFormInput -> {
                val state = remember(form, field) {
                    FormTextFieldState(
                        featureFormElement = field,
                        form = form,
                        context = context,
                        scope = scope
                    )
                }
                FormTextField(
                    state = state
                )
            }
        
            is TextBoxFormInput -> {
                val state = remember(form, field) {
                    FormTextFieldState(
                        featureFormElement = field,
                        form = form,
                        context = context,
                        scope = scope
                    )
                }
                FormTextField(
                    state = state
                )
            }
        
            is DateTimePickerFormInput -> {
                val state = remember(form, field) {
                    DateTimeFieldState(
                        formElement = field,
                        form = form,
                        scope = scope
                    )
                }
                DateTimeField(
                    state = state
                )
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
