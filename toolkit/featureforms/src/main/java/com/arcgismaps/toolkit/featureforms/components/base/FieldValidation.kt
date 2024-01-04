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

package com.arcgismaps.toolkit.featureforms.components.base

internal sealed class ValidationErrorState {
    object NoError: ValidationErrorState()
    object Required: ValidationErrorState()
    object MinMaxCharConstraint: ValidationErrorState()
    object ExactCharConstraint: ValidationErrorState()
    object MaxCharConstraint: ValidationErrorState()
    object MinNumericConstraint: ValidationErrorState()
    object MaxNumericConstraint: ValidationErrorState()
    object MinMaxNumericConstraint: ValidationErrorState()
    object NotANumber: ValidationErrorState()
    object NotAWholeNumber: ValidationErrorState()
}
