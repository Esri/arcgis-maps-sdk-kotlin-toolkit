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

import kotlinx.serialization.Serializable


/**
 * Arcade expression used in the form.
 */
@Serializable
public class FeatureFormExpressionInfo (
    var expression: String,
    var name: String,
    var returnType: String,
    var title: String
) {
    //region Properties
    
    /**
     * The Arcade expression.
     */
    
    
    /**
     * Unique identifier for the expression.
     */
    
    //  private lateinit var _returnType: Any
    /**
     * Return type of the Arcade expression. This can be determined by the authoring
     * client by executing the expression using a sample feature(s), although it can
     * be corrected by the user.
     */
//    public var returnType: Any
//        get() {
//            return if (::_returnType.isInitialized) {
//                _returnType
//            } else {
//                _returnType = coreFeatureFormExpressionInfo.returnType.convertToPublic()!!
//                _returnType
//            }
//        }
//        set(value) {
//            _returnType = value
//            coreFeatureFormExpressionInfo.returnType = _returnType.coreElement
//        }
    /**
     * Title of the expression.
     */
   
}

