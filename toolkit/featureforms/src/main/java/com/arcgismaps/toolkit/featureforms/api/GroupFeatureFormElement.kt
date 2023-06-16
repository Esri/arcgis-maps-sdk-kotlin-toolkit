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
 * Defines a container that holds a set of form elements that can be expanded, collapsed, or displayed together.
 */
@Serializable
@SerialName("group")
public data class GroupFeatureFormElement constructor(
    public val formElements: List<FeatureFormElement>,
    public var initialState: FeatureFormGroupState,
    override var description: String = "",
    override var label: String = "",
    override var visibilityExpressionName: String = ""
) : FeatureFormElement
    //region Properties
