package com.arcgismaps.toolkit.featureforms.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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


/**
 * Defines the desired user interface is a list of values in a drop-down that supports typing
 * to filter. Only one value can be selected at a time.
 */
@Serializable
@SerialName("combo-box")
public data class ComboBoxFeatureFormInput constructor(
    public var noValueOptionLabel: String = "",
    public var showNoValueOption: Boolean = false
) : FeatureFormInput {
    //region Properties

    /**
     * The text used to represent a null value.
     */
 
    /**
     * This property only applies to fields that support null values. It indicates whether to
     * display a null value option. If not provided, the default value is true.
     */

}

