package com.arcgismaps.toolkit.featureforms.components.text

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput

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
 */
internal fun FormTextFieldState(featureFormElement: FieldFeatureFormElement): FormTextFieldState =
    FormTextFieldStateImpl(featureFormElement)

/**
 * Default implementation for the [FormTextFieldState]. See [FormTextFieldState()] for the factory.
 */
private class FormTextFieldStateImpl(
    featureFormElement: FieldFeatureFormElement
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
    val helperText = buildString {
        if (minLength > 0 && maxLength > 0) {
            if (minLength == maxLength) {
                append("Enter $minLength characters")
            } else {
                append("Enter $minLength to $maxLength characters")
            }
        } else if (maxLength > 0) {
            append("Maximum $maxLength characters")
        }
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
}
