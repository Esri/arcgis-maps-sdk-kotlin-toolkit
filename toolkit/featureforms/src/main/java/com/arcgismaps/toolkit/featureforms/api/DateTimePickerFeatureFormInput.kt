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
//import java.time.Instant
//import com.arcgismaps.toolkit.featureformsapp.featureform.FeatureFormInputImpl
//import com.arcgismaps.internal.wrapping.WrappingFactory
//import com.arcgismaps.internal.platformextensions.coreDateTime
//import com.arcgismaps.internal.jni.CoreDateTimePickerFeatureFormInput
//import com.arcgismaps.internal.coreextensions.instant
//
///**
// * A user interface for a calendar date picker.
// */
//public class DateTimePickerFeatureFormInput internal constructor(
//    internal val coreDateTimePickerFeatureFormInput: CoreDateTimePickerFeatureFormInput
//) : FeatureFormInput by FeatureFormInputImpl(coreDateTimePickerFeatureFormInput) {
//    //region Properties
//
//    /**
//     * Indicates if the date/time picker should provide an option to select the time.
//     * If not provided, the default value is false.
//     */
//    public var includeTime: Boolean
//        get() {
//            return coreDateTimePickerFeatureFormInput.includeTime
//        }
//        set(value) {
//            coreDateTimePickerFeatureFormInput.includeTime = value
//        }
//    private lateinit var _max: Instant
//    /**
//     * The maximum date to allow.
//     */
//    public var max: Instant
//        get() {
//            return if (::_max.isInitialized) {
//                _max
//            } else {
//                _max = coreDateTimePickerFeatureFormInput.max.instant
//                _max
//            }
//        }
//        set(value) {
//            _max = value
//            coreDateTimePickerFeatureFormInput.max = _max.coreDateTime
//        }
//    private lateinit var _min: Instant
//    /**
//     * The minimum date to allow.
//     */
//    public var min: Instant
//        get() {
//            return if (::_min.isInitialized) {
//                _min
//            } else {
//                _min = coreDateTimePickerFeatureFormInput.min.instant
//                _min
//            }
//        }
//        set(value) {
//            _min = value
//            coreDateTimePickerFeatureFormInput.min = _min.coreDateTime
//        }
//    //endregion Properties
//
//    //region Factories
//
//    internal object Factory : WrappingFactory<CoreDateTimePickerFeatureFormInput, DateTimePickerFeatureFormInput>(::DateTimePickerFeatureFormInput)
//    //endregion Factories
//
//}
//
