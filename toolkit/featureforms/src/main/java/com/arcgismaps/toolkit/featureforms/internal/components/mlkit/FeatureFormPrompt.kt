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
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class FeatureFormPrompt(
    private val featureForm: FeatureForm
) {

    private var _prompt: String = ""
    val prompt : String
        get() = _prompt

    init {
        _prompt = generatePrompt()
    }

    private fun generatePrompt(): String {
        return buildString {
            append("The title of the form is ${featureForm.title.value}")
            featureForm.elements.forEach { element ->
                if (element is FieldFormElement && element.isEditable.value && element.isVisible.value) {
                    val elementPrompt = FieldFormElementPrompt.from(element)
                    Log.e("TAG", "generatePrompt: $elementPrompt", )
                    append("\nlabel:${element.label}")
                    element.description.runIfNotEmpty { description ->
                        append(", description:$description")
                    }
                    element.hint.runIfNotEmpty { hint ->
                        append(", hint:$hint")
                    }
                    append(", field type: ${element.fieldType}")
                }
            }
        }
    }

    suspend fun setAnswer(answer: String) {

    }
}

@Serializable
private class FieldFormElementPrompt(
    val label: String,
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
                description = element.description,
                hint = element.hint,
                fieldType = element.fieldType.name(),
                codedValues = codedValues
            )
        }
    }
}

private fun String.runIfNotEmpty(block: (String) -> Unit) {
    if (this.isNotEmpty()) block(this)
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
