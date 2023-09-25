package com.arcgismaps.toolkit.featureforms.components.combo

import android.content.Context
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState

internal class ComboBoxFieldState(
    formElement: FieldFormElement,
    form: FeatureForm,
    context: Context
) : BaseFieldState by BaseFieldState(formElement, form) {

    val codedValues: List<CodedValue> = form.getCodedValues(formElement.fieldName)

    private val showNoValueOption: FormInputNoValueOption =
        (formElement.input as ComboBoxFormInput).noValueOption

    private val noValueLabel: String =
        (formElement.input as ComboBoxFormInput).noValueLabel

    override val placeholder: String = if (isRequired) {
        context.getString(R.string.enter_value)
    } else if (showNoValueOption == FormInputNoValueOption.Show) {
        noValueLabel.ifEmpty { context.getString(R.string.no_value) }
    } else ""
}

internal fun FeatureForm.getCodedValues(fieldName: String): List<CodedValue> {
    return feature.featureTable?.getField(fieldName)?.let {
        val domain = it.domain as? CodedValueDomain
        domain?.codedValues ?: emptyList()
    } ?: emptyList()
}
