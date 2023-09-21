package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption

internal interface ComboBoxFieldState {
    val selectedValue: State<CodedValue?>
    val codedValues: List<CodedValue>
    val label: String
    val placeholder: String
    val isRequired: Boolean
    val isEditable: Boolean

    fun setValue(value : CodedValue)
}

private class ComboBoxFieldStateImpl(
    formElement: FieldFormElement,
    form: FeatureForm
) : ComboBoxFieldState {
    override val codedValues: List<CodedValue> = form.getCodedValues(formElement.fieldName)

    private val _selectedValue: MutableState<CodedValue?> = mutableStateOf(
        codedValues.firstOrNull {
            it.name == formElement.value
        }
    )
    override val selectedValue: State<CodedValue?> = _selectedValue

    override val isRequired: Boolean = formElement.requiredExpressionName.isNotEmpty()

    override val isEditable: Boolean = formElement.editableExpressionName.isNotEmpty()

    val showNoValueOption: FormInputNoValueOption =
        (formElement.input as ComboBoxFormInput).noValueOption

    val noValueLabel: String =
        (formElement.input as ComboBoxFormInput).noValueLabel

    override val label: String = formElement.label

    override val placeholder: String = if (isRequired) {
        "Enter Value"
    } else if (showNoValueOption == FormInputNoValueOption.Show) {
        noValueLabel.ifEmpty { "No Value" }
    } else ""

    override fun setValue(value: CodedValue) {
        _selectedValue.value = value
    }
}

internal fun ComboBoxFieldState(
    formElement: FieldFormElement,
    form: FeatureForm
): ComboBoxFieldState =
    ComboBoxFieldStateImpl(formElement, form)

internal fun FeatureForm.getCodedValues(fieldName: String): List<CodedValue> {
    return feature.featureTable?.getField(fieldName)?.let {
        val domain = it.domain as? CodedValueDomain
        domain?.codedValues ?: emptyList()
    } ?: emptyList()
}
