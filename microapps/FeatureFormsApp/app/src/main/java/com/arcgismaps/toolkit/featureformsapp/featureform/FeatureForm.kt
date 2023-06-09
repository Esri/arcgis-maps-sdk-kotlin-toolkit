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

package com.arcgismaps.toolkit.featureformsapp.featureform

import com.arcgismaps.data.ArcGISFeature

/**
 * Defines the form configuration when a user edits a feature.
 */
public class FeatureForm(
    val description: String,
    val expressionInfos: List<FeatureFormExpressionInfo>,
    val feature: ArcGISFeature,
    val featureFormDefinition: FeatureFormDefinition,
    val formElements: List<FeatureFormElement>,
    val preserveFieldValuesWhenHidden: Boolean,
    val title: String
) {
    //region Properties

    /**
     * An array of FeatureFormExpressionInfo objects that represent the Arcade expressions used in the form.
     */
    
    /**
     * The feature form definition used by the form.
     * The definition will be retrieved from the table associated with the feature.
     */
   
    /**
     * An array of FeatureFormElement objects that represent an ordered list of form elements.
     */
   
    /**
     * Determines whether a previously visible formFieldElement value is retained or
     * cleared when a visibilityExpression applied on the formFieldElement or its parent
     * formGroupElement evaluates to false. Default is false.
     */
    
  
    /**
     * The form title.
     */
   //endregion Properties
    
    //region Constructors
    
    /**
     * Create a FeatureForm with a feature and feature form definition.
     */
//    public constructor(feature: ArcGISFeature, featureFormDefinition: FeatureFormDefinition) : this(CoreFeatureForm(feature.coreArcGISFeature, featureFormDefinition.coreFeatureFormDefinition)) {
//        _feature = feature
//        _featureFormDefinition = featureFormDefinition
//    }
    
    //endregion Constructors
    
    //region Methods
    
    /**
     * Asynchronously evaluates all expressions available in a [FeatureFormDefinition].
     * The expression can include Arcade FeatureSet functions, which need to be evaluated asynchronously.
     */
//    public fun evaluateExpressionsAsync(): Unit =
//        coreFeatureForm.evaluateExpressionsAsync()
    //endregion Methods
    
    //region Factories
    
   
    //endregion Factories
    
}
