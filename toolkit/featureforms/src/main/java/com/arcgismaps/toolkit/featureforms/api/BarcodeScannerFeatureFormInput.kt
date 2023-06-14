package com.arcgismaps.toolkit.featureforms.api///*
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
//import com.arcgismaps.internal.jni.CoreBarcodeScannerFeatureFormInput
//import com.arcgismaps.toolkit.featureforms.api.FeatureFormInput
//
///**
// * A user interface for a barcode or QR code scanner. If the client
// * does not support barcode scanning, a single-line text box should be used.
// */
//public class BarcodeScannerFeatureFormInput internal constructor(
//    internal val coreBarcodeScannerFeatureFormInput: CoreBarcodeScannerFeatureFormInput
//) : FeatureFormInput by FeatureFormInputImpl(coreBarcodeScannerFeatureFormInput) {
//    //region Properties
//
//    /**
//     * This represents the maximum number of characters allowed. This only applies for
//     * string fields. If not supplied, the value is derived from the length property of
//     * the referenced field in the service.
//     */
//    public var maxLength: Int
//        get() {
//            return coreBarcodeScannerFeatureFormInput.maxLength
//        }
//        set(value) {
//            coreBarcodeScannerFeatureFormInput.maxLength = value
//        }
//    /**
//     * This represents the minimum number of characters allowed. This only applies for string fields.
//     * If not supplied, the value is 0, meaning there is no minimum constraint.
//     */
//    public var minLength: Int
//        get() {
//            return coreBarcodeScannerFeatureFormInput.minLength
//        }
//        set(value) {
//            coreBarcodeScannerFeatureFormInput.minLength = value
//        }
//    //endregion Properties
//
//    //region Factories
//
//    internal object Factory : WrappingFactory<CoreBarcodeScannerFeatureFormInput, BarcodeScannerFeatureFormInput>(::BarcodeScannerFeatureFormInput)
//    //endregion Factories
//
//}
//
