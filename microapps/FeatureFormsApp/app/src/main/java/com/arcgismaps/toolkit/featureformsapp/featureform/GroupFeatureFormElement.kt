///*
// COPYRIGHT 1995-2023 ESRI
//
// TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
// Unpublished material - all rights reserved under the
// Copyright Laws of the United States and applicable international
// laws, treaties, and conventions.
//
// For additional information, contact:
// Environmental Systems Research Institute, Inc.
// Attn: Contracts and Legal Services Department
// 380 New York Street
// Redlands, California, 92373
// USA
//
// email: contracts@esri.com
// */
//
//package com.arcgismaps.toolkit.featureformsapp.featureform
//
//import com.arcgismaps.toolkit.featureformsapp.featureform.createCoreArray
//import com.arcgismaps.toolkit.featureformsapp.featureform.FeatureFormElementImpl
//import com.arcgismaps.internal.wrapping.WrappingFactory
//import com.arcgismaps.internal.jni.CoreGroupFeatureFormElement
//import com.arcgismaps.internal.jni.CoreElementType
//import com.arcgismaps.internal.jni.CoreElement
//import com.arcgismaps.internal.collections.convertToPublic
//import com.arcgismaps.internal.collections.ListImpl
//
///**
// * Defines a container that holds a set of form elements that can be expanded, collapsed, or displayed together.
// */
//public class GroupFeatureFormElement internal constructor(
//    internal val coreGroupFeatureFormElement: CoreGroupFeatureFormElement
//) : FeatureFormElement by FeatureFormElementImpl(coreGroupFeatureFormElement) {
//    //region Properties
//
//    /**
//     * An array of Form Element objects that represent an ordered list of form elements.
//     * Nested group elements are not supported.
//     */
//    public val formElements: List<FeatureFormElement>
//        get() {
//            return coreGroupFeatureFormElement.formElements.convertToPublic()
//        }
//    /**
//     * Defines if the group should be expanded or collapsed when the form is
//     * initially displayed. If not provided, the default value is expanded.
//     */
//    public var initialState: FeatureFormGroupState
//        get() {
//            return FeatureFormGroupState.Factory.convertToPublic(coreGroupFeatureFormElement.initialState)
//        }
//        set(value) {
//            coreGroupFeatureFormElement.initialState = value.coreFeatureFormGroupState
//        }
//    //endregion Properties
//
//    //region Factories
//
//    internal object Factory : WrappingFactory<CoreGroupFeatureFormElement, GroupFeatureFormElement>(::GroupFeatureFormElement)
//    //endregion Factories
//
//}
//
//internal val GroupFeatureFormElement.coreElementForGroupFeatureFormElement: CoreElement
//    get() = CoreElement.fromGroupFeatureFormElement(coreGroupFeatureFormElement)
