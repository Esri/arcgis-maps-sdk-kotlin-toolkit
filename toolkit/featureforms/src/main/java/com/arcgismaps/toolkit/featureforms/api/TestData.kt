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
                        "description": "Description TextBox",
                        "label": "TextBox - Single Line",
                        "type": "field",
                        "editableExpression": "expr/system/true",
                        "fieldName": "TextBox_Single_Line",
                        "hint": "Placeholder Text",
                        "inputType": {
                            "type": "text-box",
                            "maxLength": 256,
                            "minLength": 0
                        }
                    },
                    {
                        "description": "Description for TextArea",
                        "label": "TextArea - Multiline",
                        "type": "field",
                        "editableExpression": "expr/system/true",
                        "fieldName": "TextArea_Multiline",
                        "hint": "Placeholder Text",
                        "inputType": {
                            "type": "text-area",
                            "maxLength": 1000,
                            "minLength": 0
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
                "title": "PointFeature"
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

