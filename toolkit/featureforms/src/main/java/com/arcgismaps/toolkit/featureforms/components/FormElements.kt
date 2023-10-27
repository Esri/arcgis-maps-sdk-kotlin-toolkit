package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.GroupFormElement
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
internal fun FieldElement(
    state: BaseFieldState,
    onDialogRequest: () -> Unit = {}
) {
    val visible by state.isVisible.collectAsState()
    if (visible) {
        when (state) {
            is FormTextFieldState -> {
                FormTextField(state = state)
            }

            is DateTimeFieldState -> {
                DateTimeField(
                    state = state,
                    onDialogRequest = onDialogRequest
                )
            }

            is SwitchFieldState -> {
                if (!state.fallback) {
                    SwitchField(state = state)
                } else {
                    ComboBoxField(
                        state = state,
                        onDialogRequest = onDialogRequest
                    )
                }
            }

            is RadioButtonFieldState -> {
                if (state.shouldFallback()) {
                    ComboBoxField(state = state)
                } else {
                    RadioButtonField(state = state)
                }
            }

            is CodedValueFieldState -> {
                ComboBoxField(
                    state = state,
                    onDialogRequest = onDialogRequest
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
