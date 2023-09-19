package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement

internal interface ComboBoxFieldState {
    val selectedValue: State<CodedValue?>
    val codedValues: List<CodedValue>
    val label : String
    val placeholder : String
    val isEditable: Boolean
}

private class ComboBoxFieldStateImpl(
    formElement: FieldFormElement,
    form: FeatureForm
) : ComboBoxFieldState {
    override val codedValues: List<CodedValue> = form.getCodedValues(formElement.fieldName)

    override val selectedValue: State<CodedValue?> = mutableStateOf(
        codedValues.firstOrNull {
            it.name == formElement.value
        }
    )

    override val label: String = formElement.label

    override val placeholder: String
        get() = TODO("Not yet implemented")
    override val isEditable: Boolean = formElement.editableExpressionName.isNotEmpty()

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
