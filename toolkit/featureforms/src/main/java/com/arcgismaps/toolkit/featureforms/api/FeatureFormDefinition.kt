/*
 COPYRIGHT 1995-2023 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.api

import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Feature
import com.arcgismaps.data.FieldType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Defines the form configuration when a user edits a feature.
 */
@Serializable
public data class FeatureFormDefinition(
    var description: String = "",
    val expressionInfos: List<FeatureFormExpressionInfo>,
    val formElements: List<FeatureFormElement>,
    var preserveFieldValuesWhenHidden: Boolean = true,
    var title: String,

    ) {

    @Transient
    var feature: ArcGISFeature? = null
    
    
    /**
     * Retrieve the value of a [FieldFeatureFormElement] from the [FeatureFormDefinition].
     * This call is likely to be pushed into core.
     */
    public fun getElementValue(formElement: FieldFeatureFormElement): Any? {
        return feature?.attributes?.get(formElement.fieldName)
    }
    
    
    /**
     * Set the value in the feature's attribute map. This call can only be made when a transaction is open.
     * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
     * and refresh the feature.
     *
     * This call is likely to be pushed into core.
     */
    public fun editValue(formElement: FieldFeatureFormElement, value: Any?) {
        feature?.castAndSetAttributeValue(value, formElement.fieldName)
    }
    
    
    //region Companion Object
    
    public companion object {
        private val jsonDecoder = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    
        public fun fromJsonOrNull(json: JsonObject): FeatureFormDefinition? {
            return try {
                jsonDecoder.decodeFromJsonElement(json)
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }
        
        public fun fromJsonOrNull(jsonString: String): FeatureFormDefinition? = try {
            jsonDecoder.decodeFromString(serializer(), jsonString)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
    //endregion Companion Object
    
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

