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
//import com.arcgismaps.internal.jni.CoreSwitchFeatureFormInput
//import com.arcgismaps.data.CodedValue
//
///**
// * A user interface for a binary switch, or toggle. This should be used when
// * selecting between two options.
// */
//public class SwitchFeatureFormInput internal constructor(
//    internal val coreSwitchFeatureFormInput: CoreSwitchFeatureFormInput
//) : FeatureFormInput by FeatureFormInputImpl(coreSwitchFeatureFormInput) {
//    //region Properties
//
//    private lateinit var _offValue: CodedValue
//    /**
//     * The coded value when switch state is off.
//     */
//    public var offValue: CodedValue
//        get() {
//            return if (::_offValue.isInitialized) {
//                _offValue
//            } else {
//                _offValue = CodedValue.Factory.convertToPublic(coreSwitchFeatureFormInput.offValue)!!
//                _offValue
//            }
//        }
//        set(value) {
//            _offValue = value
//            coreSwitchFeatureFormInput.offValue = _offValue.coreCodedValue
//        }
//    private lateinit var _onValue: CodedValue
//    /**
//     * The coded value when switch state is on.
//     */
//    public var onValue: CodedValue
//        get() {
//            return if (::_onValue.isInitialized) {
//                _onValue
//            } else {
//                _onValue = CodedValue.Factory.convertToPublic(coreSwitchFeatureFormInput.onValue)!!
//                _onValue
//            }
//        }
//        set(value) {
//            _onValue = value
//            coreSwitchFeatureFormInput.onValue = _onValue.coreCodedValue
//        }
//    //endregion Properties
//
//    //region Factories
//
//    internal object Factory : WrappingFactory<CoreSwitchFeatureFormInput, SwitchFeatureFormInput>(::SwitchFeatureFormInput)
//    //endregion Factories
//
//}
//
