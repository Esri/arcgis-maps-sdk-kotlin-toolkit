package com.arcgismaps.toolkit.featureforms.api

import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public class TestData {
    public companion object {
        public val formInfo: String = """
        {
          "formElements": [
            {
              "label": "Height of the tree",
              "description": "The height of the tree in feet",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "height",
              "inputType": {
                "type": "text-box",
                "minLength": 0
              }
            },
            {
              "label": "Name of the tree",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "Name",
              "inputType": {
                "type": "text-box",
                "minLength": 0,
                "maxLength": 20
              }
            },
            {
              "description": "enter some information",
              "label": "Singleline Text",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "Singleline_Text",
              "inputType": {
                "type": "text-box",
                "maxLength": 256,
                "minLength": 0
              },
              "hint": "enter here.."
            },
            {
              "description": "Describe the tree condition including any signs of disease",
              "label": "Notes",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "Notes",
              "inputType": {
                "type": "text-area",
                "maxLength": 200,
                "minLength": 20
              },
              "hint": "Enter a brief comment"
            },
            {
              "description": "Default Value is 'TEXT', only shows when new feature is created.",
              "label": "Default Value",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "Default_Value",
              "inputType": {
                "type": "text-box",
                "maxLength": 256,
                "minLength": 0
                }
            },
            {
              "description": "Minimum length of 5, Maximum length of 10",
              "label": "Min Length Max Length",
              "type": "field",
              "editableExpression": "expr/system/true",
              "fieldName": "Min_Length_Max_Length",
              "inputType": {
                "type": "text-box",
                "maxLength": 10,
                "minLength": 5
              }
            }
          ],
          "expressionInfos": [
            {
              "expression": "false",
              "name": "expr/system/false",
              "returnType": "boolean",
              "title": "False"
            },
            {
              "expression": "true",
              "name": "expr/system/true",
              "returnType": "boolean",
              "title": "True"
            }
          ],
          "title": "simpleFeatureForm"
        }
    """.trimIndent()
    }
}

@Serializable
public data class OperationalLayer(@SerialName("formInfo") val featureFormDefinition: FeatureFormDefinition)

@Serializable
public data class MapInfo(val operationalLayers: List<OperationalLayer>)

private val jsonDecoder = Json { ignoreUnknownKeys = true }

public val ArcGISMap.featureFormDefinition: FeatureFormDefinition
    get() {
        val json = toJson()
        val mapInfo = jsonDecoder.decodeFromString<MapInfo>(json)
        return mapInfo.operationalLayers.first().featureFormDefinition
    }

internal val ArcGISFeatureTable.formInfoJson: JsonObject?
    get() {
        val jsonMap = unsupportedJson["formInfo"] as? Map<*, *> ?: return null
        return jsonMap.toJsonObject()
    }

// Currently Kotlin Serialization does not support Map<String, Any> so they suggest we use
// this code to convert the Map<String, Any> to a JsonObject that Kotlin Serialization
// knows about.
// https://github.com/Kotlin/kotlinx.serialization/issues/746#issuecomment-737000705
//

private fun Map<*, *>.toJsonObject(): JsonObject {
    val map = mutableMapOf<String, JsonElement>()
    this.forEach {
        if (it.key is String) {
            map[it.key as String] = it.value.toJsonElement()
        }
    }
    return JsonObject(map)
}

private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Array<*> -> this.toJsonArray()
        is List<*> -> this.toJsonArray()
        is Map<*, *> -> this.toJsonObject()
        is JsonElement -> this
        else -> JsonNull
    }
}

private fun Array<*>.toJsonArray(): JsonArray {
    val array = mutableListOf<JsonElement>()
    this.forEach { array.add(it.toJsonElement()) }
    return JsonArray(array)
}

private fun List<*>.toJsonArray(): JsonArray {
    val array = mutableListOf<JsonElement>()
    this.forEach { array.add(it.toJsonElement()) }
    return JsonArray(array)
}

