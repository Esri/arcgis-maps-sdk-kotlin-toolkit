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
import kotlinx.serialization.json.JsonClassDiscriminator


/**
 * An interface containing properties common to feature form elements.
 */
@Serializable
public sealed interface FeatureFormElement {
    //region Properties
    
    /**
     * A string that describes the element in detail.
     */
    public var description: String
    /**
     * A string indicating what the element represents. If not supplied, the label is derived
     * from the alias property in the referenced field in the service.
     */
    public var label: String
    /**
     * A reference to an Arcade expression that returns a boolean value. When this expression evaluates to true,
     * the element is displayed. When the expression evaluates to false the element is not displayed. If no expression
     * is provided, the default behavior is that the element is displayed. Care must be taken when defining a
     * visibility expression for a non-nullable field i.e. to make sure that such fields either have default values
     * or are made visible to users so that they can provide a value before submitting the form.
     */
    public var visibilityExpressionName: String
    
    //endregion Properties
    
}
