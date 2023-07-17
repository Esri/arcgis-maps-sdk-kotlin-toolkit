package com.arcgismaps.toolkit.featureforms.components.text

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.Feature
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.components.FieldElement

/**
 * State for the [FormTextField]
 */
internal interface FormTextFieldState {
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
     * Indicates if the [FormTextField] is single line or multi-line.
     */
    val singleLine: Boolean
    
    /**
     * Minimum length constraint for the [FormTextField].
     */
    val minLength: Int
    
    /**
     * Maximum length constraint for the [FormTextField].
     */
    val maxLength: Int
    
    /**
     * Current value state for the [FormTextField].
     */
    val value: State<String>
    
    /**
     * State for the supporting text that gets displayed under the [FormTextField].
     */
    val supportingText: State<String>
    
    /**
     * The current content length based on the [value] state and expressed as a string. This is
     * only available when the [minLength] or [maxLength] properties are non-zero.
     */
    val contentLength: State<String>
    
    /**
     * State that indicates if the [FormTextField] is currently focused.
     */
    val isFocused: State<Boolean>
    
    /**
     * State that indicates if there input validation on the [FormTextField] caused an error. Check
     * [errorMessage] for the actual error information.
     */
    val hasError: State<Boolean>
    
    /**
     * State that indicates the current error message if there is any error.
     */
    val errorMessage: State<String>
    
    /**
     * Callback to update the current value of the FormTextFieldState to the given [input].
     */
    fun onValueChanged(input: String)
    
    /**
     * Callback to set the current focus value to the given [focus].
     */
    fun onFocusChanged(focus: Boolean)
}

/**
 * Factory function to create a [FormTextFieldState] using the [featureFormElement].
 *
 * @param featureFormElement the form element.
 * @param context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 */
internal fun FormTextFieldState(featureFormElement: FieldFeatureFormElement,
                                formDefinition: FeatureFormDefinition,
                                context: Context): FormTextFieldState =
    FormTextFieldStateImpl(featureFormElement, formDefinition, context)

/**
 * Default implementation for the [FormTextFieldState]. See [FormTextFieldState()] for the factory.
 *
 * @param featureFormElement the form element.
 * @property context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 */
private class FormTextFieldStateImpl(
    private val featureFormElement: FieldFeatureFormElement,
    private val featureFormDefinition: FeatureFormDefinition,
    private val context: Context
) : FormTextFieldState {
    private val _value = mutableStateOf(featureFormElement.value)
    override val value: State<String> = _value
    
    private val _isFocused = mutableStateOf(false)
    override val isFocused: State<Boolean> = _isFocused
    
    private val _errorMessage = mutableStateOf("")
    override val errorMessage: State<String> = _errorMessage
    
    private val _hasError = mutableStateOf(false)
    override val hasError: State<Boolean> = _hasError
    
    // set the label from the FieldFeatureFormElement
    override val label = featureFormElement.label
    
    // set the description from the FieldFeatureFormElement
    override val description = featureFormElement.description
    
    // set the placeholder from the FieldFeatureFormElement
    override val placeholder = featureFormElement.hint
    
    // indicates singleLine only if TextBoxFeatureFormInput
    override val singleLine = featureFormElement.inputType is TextBoxFeatureFormInput
    
    // fetch the minLength based on the featureFormElement.inputType
    override val minLength = when (featureFormElement.inputType) {
        is TextAreaFeatureFormInput -> featureFormElement.inputType.minLength
        is TextBoxFeatureFormInput -> featureFormElement.inputType.minLength
        else -> throw IllegalArgumentException()
    }.toInt()
    
    // fetch the maxLength based on the featureFormElement.inputType
    override val maxLength = when (featureFormElement.inputType) {
        is TextAreaFeatureFormInput -> featureFormElement.inputType.maxLength
        is TextBoxFeatureFormInput -> featureFormElement.inputType.maxLength
        else -> throw IllegalArgumentException()
    }.toInt()
    
    // build helper text
    val helperText =
        if (minLength > 0 && maxLength > 0) {
            if (minLength == maxLength) {
                context.getString(R.string.enter_n_chars, minLength)
            } else {
                context.getString(R.string.enter_min_to_max_chars, minLength, maxLength)
            }
        } else if (maxLength > 0) {
            context.getString(R.string.maximum_n_chars, maxLength)
        } else {
            context.getString(R.string.maximum_n_chars, 254)
            // TODO: when consuming the core API throw here and remove the line above.
            //throw IllegalStateException("invalid form data or attribute: text field must have a nonzero max length")
        }
    
    // supporting text will depend on multiple other states. If there is an error, it will display
    // error message. Otherwise description is displayed, unless it is empty in which case
    // the helper text is displayed when the field is focused.
    override val supportingText = derivedStateOf {
        if (_hasError.value) _errorMessage.value else {
            description.ifEmpty {
                if (_isFocused.value) helperText else ""
            }
        }
    }
    
    // derive the content length from the current value only if any length constraint is set
    override val contentLength = derivedStateOf {
        if (minLength > 0 || maxLength > 0) {
            _value.value.length.toString()
        } else ""
    }
    
    override fun onValueChanged(input: String) {
       editValue(input)
        _value.value = input
        validateLength()
    }
    
    override fun onFocusChanged(focus: Boolean) {
        _isFocused.value = focus
    }
    
    /**
     * Validates the current [value]'s length based on the [minLength] and [maxLength] and sets the
     * [hasError] and [errorMessage] if there was an error in validation.
     */
    private fun validateLength() {
        _hasError.value = (_value.value.length !in minLength..maxLength).also {
            if (it) {
                _errorMessage.value = helperText
            }
        }
    }
    
    /**
     * Set the value in the feature's attribute map.
     * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
     * and refresh the feature.
     */
    private fun editValue(value: Any?) {
        featureFormDefinition.editValue(featureFormElement, value)
    }
}

/**
 * Set the value in the feature's attribute map. This call can only be made when a transaction is open.
 * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
 * and refresh the feature.
 *
 * This call is likely to be pushed into core.
 */
private fun FeatureFormDefinition.editValue(formElement: FieldFeatureFormElement, value: Any?) {
    feature?.castAndSetAttributeValue(value, formElement.fieldName)
}

private fun Feature.castAndSetAttributeValue(value: Any?, key: String) {
    val field = featureTable?.getField(key) ?: run {
        attributes[key] = value
        return
    }
    
    var finalValue = value
    when (field.fieldType) {
        FieldType.Int16 -> {
            finalValue = when (value) {
                is String -> value.toIntOrNull()?.toShort()
                is Int -> value.toShort()
                is Double -> value.roundToInt().toShort()
                else -> null
            }
        }
        FieldType.Int32 -> {
            finalValue = when (value) {
                is String -> value.toIntOrNull()
                is Int -> value
                is Double -> value.roundToInt()
                else -> null
            }
        }
        FieldType.Int64 -> {
            finalValue = when (value) {
                is String -> value.toLongOrNull()
                is Int -> value.toLong()
                is Double -> value.roundToLong()
                else -> null
            }
        }
        FieldType.Float32 -> {
            finalValue = when (value) {
                is String -> value.toFloatOrNull()
                is Int -> value.toFloat()
                is Double -> value.toFloat()
                else -> null
            }
        }
        FieldType.Float64 -> {
            finalValue = when (value) {
                is String -> value.toDoubleOrNull()
                is Int -> value.toDouble()
                is Float -> value.toDouble()
                else -> null
            }
        }
        else -> Unit
    }
    attributes[key] = finalValue
}
