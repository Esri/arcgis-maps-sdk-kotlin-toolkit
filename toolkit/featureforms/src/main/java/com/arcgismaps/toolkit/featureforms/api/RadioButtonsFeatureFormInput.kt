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
 * A user interface for a radio button group.
 */
@Serializable
@SerialName("radio-buttons")
public data class RadioButtonsFeatureFormInput internal constructor(
    public var noValueOptionLabel: String = "",
    public var showNoValueOption: Boolean = false
) : FeatureFormInput
    //region Properties
