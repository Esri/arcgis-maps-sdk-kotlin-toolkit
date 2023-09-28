/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.components.combo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.FieldElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A class to handle the state of a [ComboBoxField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param formElement The [FieldFormElement] to create the state from.
 * @param featureForm The [FeatureForm] that the [formElement] is a part of.
 * @param context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 */
internal class ComboBoxFieldState(
    formElement: FieldFormElement,
    featureForm: FeatureForm,
    context: Context,
    scope: CoroutineScope
) : BaseFieldState by BaseFieldState(formElement, featureForm, scope) {

    /**
     * The list of coded values associated with this field.
     */
    val codedValues: List<CodedValue> = featureForm.getCodedValues(formElement.fieldName)

    /**
     * This property defines whether to display a special "no value" option if this field is
     * optional.
     */
    val showNoValueOption: FormInputNoValueOption =
        (formElement.input as ComboBoxFormInput).noValueOption

    /**
     * The custom label to use if [showNoValueOption] is enabled.
     */
    val noValueLabel: String =
        (formElement.input as ComboBoxFormInput).noValueLabel

    override val placeholder = if (isRequired.value) {
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
