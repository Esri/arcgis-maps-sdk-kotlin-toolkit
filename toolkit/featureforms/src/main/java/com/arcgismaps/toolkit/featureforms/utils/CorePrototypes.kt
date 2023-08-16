/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.utils

import com.arcgismaps.data.Feature
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * This file contains logic which will eventually be provided by core. Do not add anything to this file that isn't
 * scheduled for core implementation. This entire file will be removed before the 200.3.0 release.
 */

/**
 * Retrieve the value of a [FieldFormElement] from the [FeatureFormDefinition].
 * This call is likely to be pushed into core.
 */
@Suppress("unused")
internal fun FeatureForm.getElementValue(formElement: FieldFormElement): Any? {
    return feature.attributes[formElement.fieldName]
}

/**
 * Set the value in the feature's attribute map. This call can only be made when a transaction is open.
 * Committing the transaction will either discard this edit or persist it in the associated geodatabase,
 * and refresh the feature.
 *
 * This call is likely to be pushed into core.
 */
internal fun FeatureForm.editValue(formElement: FieldFormElement, value: Any?) {
    feature.castAndSetAttributeValue(value, formElement.fieldName)
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
