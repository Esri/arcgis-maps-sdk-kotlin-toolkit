package com.arcgismaps.toolkit.featureforms.components.text

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.getElementValue

internal interface BaseFieldState {
    /**
     * Title for the [FormTextField].
     */
    val label: String

    /**
     * Description text for the [FormTextField].
     */
    val description: String

    /**
     * Placeholder hint for the [FormTextField]
     */
    val placeholder: String

    /**
     * Current value state for the [FormTextField].
     */
    val value: State<String>

    val isEditable : Boolean

    val isRequired : Boolean

    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    fun onValueChanged(input: String)
}

private class BaseFieldStateImpl(
    private val formElement: FieldFormElement,
    private val featureForm: FeatureForm
) : BaseFieldState {

    private val _value = mutableStateOf(formElement.value.ifEmpty {
        // "prime" the value until expressions can be evaluated to populate the value.
        // TODO: remove this when the value is provided by expression evaluation.
        featureForm.getElementValue(formElement)?.toString() ?: ""
    })
    override val value: State<String> = _value

    override val description: String = formElement.description

    override val placeholder: String = formElement.hint

    override val isEditable: Boolean = formElement.editableExpressionName.isNotEmpty()

    override val isRequired: Boolean = formElement.requiredExpressionName.isNotEmpty()

    // set the label from the FieldFeatureFormElement
    // note when isRequired becomes a StateFlow, this logic will move into the compose function
    override val label = if (!isRequired) {
        formElement.label
    } else {
        "${formElement.label} *"
    }

    override fun onValueChanged(input: String) {
        editValue(input)
        _value.value = input
    }

    /**
     * Set the value in the feature's attribute map.
     * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
     * and refresh the feature.
     */
    private fun editValue(value: Any?) {
        featureForm.editValue(formElement, value)
    }
}

internal fun BaseFieldState(
    formElement: FieldFormElement,
    featureForm: FeatureForm
): BaseFieldState = BaseFieldStateImpl(formElement, featureForm)
