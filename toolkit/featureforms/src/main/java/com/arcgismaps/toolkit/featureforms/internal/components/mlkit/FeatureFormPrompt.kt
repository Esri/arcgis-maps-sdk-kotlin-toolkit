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
import androidx.compose.runtime.Stable
import com.arcgismaps.data.CodedValueDomain
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
import kotlin.time.Instant
import kotlin.time.toJavaInstant

internal class FeatureFormPrompt(
    private val formStateData: FormStateData
) {
    /**
     * Backing property for [prompt].
     */
    private var _prompt: String = ""

    /**
     * The prompt to send to the generative model. This is built lazily and cached, so it will only
     * be built once per instance of [FeatureFormPrompt].
     */
    val prompt: String
        get() = _prompt.ifEmpty {
            _prompt = buildPrompt()
            Log.e("TAG", "$_prompt: ", )
            _prompt
        }

    /**
     * Builds the prompt based on the current state of the [featureForm].
     */
    private fun buildPrompt() : String {
        val formInfo = buildString {
            appendLine("Title: ${formStateData.featureForm.title.value}")
            formStateData.stateCollection.forEach { state ->
                when(val element = state.formElement) {
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
            |Only include fields that the user is trying to fill out. If you cannot determine any 
            |fields from the user response, do not include it in the answer. The user will name a
            |field only by its label. Use the label to determine the fieldName, which is the key to 
            |use in the output. If a field has coded values, use the provided name ignoring its field 
            |type. The value should always be a string. If the field is a date or datetime field, 
            |the value should be in epoch millis.
            |</instructions>
            |##
            |<output_format>
            |Return ONLY a valid and raw JSON object.
            |Do NOT use triple backticks or use a code block.
            |An example of the output format schema is -
            |{
            |    "fieldValues": {
            |        "name": "John",
            |        "age": "42"
            |    }
            |}
            |</output_format>
            |##
        """.trimMargin("|")
    }

    suspend fun processResponse(response: FeatureFormPromptResponse) {
        formStateData.stateCollection.forEach { entry ->
            val state = entry.state
            if (state is BaseFieldState<*>) {
                val element = entry.formElement as FieldFormElement
                val value = response.fieldValues[element.fieldName]
                Log.e("TAG", "processResponse: $value", )
                if (value != null) {
                    when(state) {
                        is CodedValueFieldState -> {
                            val codedValue = state.codedValues.entries.firstOrNull {
                                it.value == value
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
            }
        }
        formStateData.evaluateExpressions()
    }
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

private fun FormElement.toPromptString(): String? {
    return when (this) {
        is FieldFormElement -> {
            if (isEditable.value && isVisible.value) {
                FieldFormElementPrompt.from(this).toString()
            } else {
                null
            }
        }
        else -> null
    }
}
