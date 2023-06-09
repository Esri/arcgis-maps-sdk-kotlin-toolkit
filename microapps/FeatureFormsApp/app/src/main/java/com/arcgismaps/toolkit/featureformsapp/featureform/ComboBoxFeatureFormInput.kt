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
//import com.arcgismaps.toolkit.featureformsapp.featureform.FeatureFormInputImpl
//import com.arcgismaps.internal.wrapping.WrappingFactory
//import com.arcgismaps.internal.jni.CoreComboBoxFeatureFormInput
//
///**
// * Defines the desired user interface is a list of values in a drop-down that supports typing
// * to filter. Only one value can be selected at a time.
// */
//public class ComboBoxFeatureFormInput internal constructor(
//    internal val coreComboBoxFeatureFormInput: CoreComboBoxFeatureFormInput
//) : FeatureFormInput by FeatureFormInputImpl(coreComboBoxFeatureFormInput) {
//    //region Properties
//
//    /**
//     * The text used to represent a null value.
//     */
//    public var noValueOptionLabel: String
//        get() {
//            return coreComboBoxFeatureFormInput.noValueOptionLabel
//        }
//        set(value) {
//            coreComboBoxFeatureFormInput.noValueOptionLabel = value
//        }
//    /**
//     * This property only applies to fields that support null values. It indicates whether to
//     * display a null value option. If not provided, the default value is true.
//     */
//    public var showNoValueOption: Boolean
//        get() {
//            return coreComboBoxFeatureFormInput.showNoValueOption
//        }
//        set(value) {
//            coreComboBoxFeatureFormInput.showNoValueOption = value
//        }
//    //endregion Properties
//
//    //region Factories
//
//    internal object Factory : WrappingFactory<CoreComboBoxFeatureFormInput, ComboBoxFeatureFormInput>(::ComboBoxFeatureFormInput)
//    //endregion Factories
//
//}
//
