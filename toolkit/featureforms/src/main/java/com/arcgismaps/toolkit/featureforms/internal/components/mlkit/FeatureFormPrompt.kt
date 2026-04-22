/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.mlkit

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.toolkit.featureforms.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * A class to build a prompt for a generative model for a [FeatureForm].
 *
 * @param formStateData The state data of the form to build the prompt from.
 */
internal class FeatureFormPrompt(
    private val formStateData: FormStateData
) {

    private var _prompt: String = ""

    /**
     * The prompt to send to the generative model.
     */
    val prompt: String
        get() {
            if (_prompt.isEmpty()) {
                _prompt = buildPrompt()
            }
            return _prompt
        }

    /**
     * Builds the prompt based on the current state of the form.
     */
    private fun buildPrompt(): String {
        val formInfo = buildString {
            appendLine("Title: ${formStateData.featureForm.title.value}")
            formStateData.stateCollection.forEach { state ->
                when (val element = state.formElement) {
                    is FieldFormElement -> {
                        state.formElement.toPromptString()?.let {
                            appendLine(it)
                        }
                    }

                    is GroupFormElement -> {
                        element.elements.forEach {
                            it.toPromptString()?.let { fieldElementPrompt ->
                                appendLine(fieldElementPrompt)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
        return """
            |<background_information>
            |You are an assistant for filling out a form. Your task is to determine which fields on the form 
            |a user is trying to fill out and what values they are trying to enter based on their speech to 
            |text response. Here is a JSON representation of the form, including details about each field 
            |that is relevant to filling it out:
            |$formInfo
            |</background_information>
            |##
            |<instructions>
            |The user's response may contain speech-to-text artifacts such as incorrect punctuation,
            |split numbers, repeated fragments, or slightly malformed grammar. Normalize the text to 
            |the most likely intended spoken meaning before extracting values.
            |
            |Examples:
            |- "4, 14 PM" means "4:14 PM"
            |- "11 30 AM" means "11:30 AM"
            |- "April 18th. 4, 14 PM" means "April 18th, 4:14 PM"
            |
            |Prefer the most natural human interpretation of the spoken input, especially for date and time fields.
            |
            |Only include fields that the user is trying to fill out. If you cannot determine any 
            |fields from the user response, do not include it in the answer. The user will name a
            |field only by its label. Use the label to determine the "fieldName". Only provide the 
            |"fieldName" in the output. 
            |
            |If a field has coded values, use the provided name ignoring its field type. The value 
            |should always be a string. 
            |
            |If the field is a DateOnly or Date field, the value should be an ISO 8601 string that 
            |represents an instant (for example, 2026-04-22T14:34:00-07:00). The user's current time zone 
            |is ${ZoneId.systemDefault().id} and should be taken into account when determining the 
            |value for date fields. 
            |
            |The current date and time is ${Clock.System.now()}. Use this context to determine the 
            |value for date fields if the user response includes relative date information 
            |(for example, "next Tuesday" or "in 3 days").
            |</instructions>
            |##
            |<output_format>
            |Return ONLY a valid and raw JSON object.
            |Return ONLY the "fieldName" and the "value" that the user is trying to enter for that field
            |An example of the output format schema is -
            |{
            |    "fieldValues": {
            |        "field_name": "John",
            |        "age": "42"
            |    }
            |}
            |</output_format>
            |##
        """.trimMargin("|")
    }

    private fun processFieldValue(state: BaseFieldState<*>, value: String) {
        // If the field is not editable or not visible, do not attempt to update its value
        if (state.isEditable.value.not() || state.isVisible.value.not()) {
            Log.e("TAG", "Field ${state.fieldName} is not editable or not visible. Skipping value update.")
            return
        }
        when (state) {
            is CodedValueFieldState -> {
                val codedValue = state.codedValues.entries.firstOrNull {
                    it.value.equals(value, ignoreCase = true)
                }
                state.onValueChanged(codedValue?.key)
            }

            is FormTextFieldState -> state.onValueChanged(value)
            is DateTimeFieldState -> {
                Instant.parseOrNull(value)?.let { instant ->
                    state.onValueChanged(instant.toJavaInstant())
                }
            }
        }
    }

    /**
     * Processes the response from the generative model and updates the form state accordingly.
     */
    fun processResponse(response: String) {
        FeatureFormPromptResponse.fromJsonOrNull(response)?.let { featureFormPromptResponse ->
            featureFormPromptResponse.fieldValues.forEach { entry ->
                Log.e("TAG", "processResponse: ${entry.key}-${entry.value}")
                formStateData.stateCollection[entry.key]?.let { state ->
                    processFieldValue(state, entry.value)
                } ?: Log.e("TAG", "No state found for fieldName: ${entry.key}")
            }
        }
    }

    companion object {
        fun Saver(
            formStateData: FormStateData
        ): Saver<FeatureFormPrompt, String> = Saver(
            save = {
                it.prompt
            },
            restore = {
                FeatureFormPrompt(formStateData).apply {
                    _prompt = it
                }
            }
        )
    }
}

@Composable
internal fun rememberFeatureFormPrompt(
    formStateData: FormStateData
): FeatureFormPrompt = rememberSaveable(
    inputs = arrayOf(formStateData),
    saver = FeatureFormPrompt.Saver(formStateData)
) {
    FeatureFormPrompt(formStateData)
}

@Serializable
internal data class FeatureFormPromptResponse(
    val fieldValues: Map<String, String>
) {
    companion object {
        fun fromJsonOrNull(json: String): FeatureFormPromptResponse? {
            return try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                Log.e("TAG", "Failed to parse response JSON: $json", e)
                null
            }
        }
    }
}

/**
 * Extension function to convert a [FormElement] to a JSON string representation for the prompt.
 * Only [FieldFormElement]s are included.
 *
 * @return A JSON string representation of the [FieldFormElement] or null if the element type is not
 * supported.
 */
private fun FormElement.toPromptString(): String? {
    return when (this) {
        is FieldFormElement -> FieldFormElementPrompt.from(this).toString()
        else -> null
    }
}

@Serializable
private class FieldFormElementPrompt(
    val label: String,
    val fieldName: String,
    val description: String,
    val hint: String,
    val fieldType: String,
    val codedValues: List<String> = emptyList(),
) {

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun from(element: FieldFormElement): FieldFormElementPrompt {
            val codedValues = when (element.input) {
                is ComboBoxFormInput -> (element.input as ComboBoxFormInput).codedValues.map {
                    it.name
                }

                is RadioButtonsFormInput -> (element.input as RadioButtonsFormInput).codedValues.map {
                    it.name
                }

                is SwitchFormInput -> {
                    val input = element.input as SwitchFormInput
                    listOf(input.onValue.name, input.offValue.name)
                }

                else -> emptyList()
            }
            return FieldFormElementPrompt(
                label = element.label,
                fieldName = element.fieldName,
                description = element.description,
                hint = element.hint,
                fieldType = element.fieldType.name(),
                codedValues = codedValues
            )
        }
    }
}

private fun FieldType.name(): String {
    return when (this) {
        FieldType.Date -> "Date"
        FieldType.Blob -> "Blob"
        FieldType.DateOnly -> "DateOnly"
        FieldType.Float32 -> "Float32"
        FieldType.Float64 -> "Float64"
        FieldType.Geometry -> "Geometry"
        FieldType.GlobalId -> "GlobalId"
        FieldType.Guid -> "Guid"
        FieldType.Int16 -> "Int16"
        FieldType.Int32 -> "Int32"
        FieldType.Int64 -> "Int64"
        FieldType.Oid -> "Oid"
        FieldType.Raster -> "Raster"
        FieldType.Text -> "Text"
        FieldType.TimeOnly -> "TimeOnly"
        FieldType.TimestampOffset -> "TimestampOffset"
        else -> "Unknown"
    }
}
