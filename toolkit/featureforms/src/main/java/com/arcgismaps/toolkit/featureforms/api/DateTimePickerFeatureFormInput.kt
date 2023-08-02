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
 * A user interface for a calendar date picker.
 */
@Serializable
@SerialName("datetime-picker")
public class DateTimePickerFeatureFormInput constructor(
    public var includeTime: Boolean = false,
    public var max: Float? = null,
    public var min: Float? = null
    
) : FeatureFormInput {
    //region Properties

    /**
     * Indicates if the date/time picker should provide an option to select the time.
     * If not provided, the default value is false.
     */
  
   

}

