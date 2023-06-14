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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user interface for a single-line text box.
 */
@SerialName("text-box")
@Serializable
public class TextBoxFeatureFormInput internal constructor(
    public var maxLength: Int,
    public var minLength: Int
) : FeatureFormInput
    //region Properties
    
    /**
     * This represents the maximum number of characters allowed. This only applies for string fields.
     * If not supplied,the value is derived from the length property of the referenced field in the service.
     */
    
    /**
     * This represents the minimum number of characters allowed. This only applies for string fields.
     * If not supplied, the value is 0, meaning there is no minimum constraint.
     */
    
    //endregion Properties
    
    //region Factories
    
  
    //endregion Factories
    


