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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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

