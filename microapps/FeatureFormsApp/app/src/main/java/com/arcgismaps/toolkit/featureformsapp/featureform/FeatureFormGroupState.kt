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
//import com.arcgismaps.internal.jni.CoreFeatureFormGroupState
//
///**
// * The list of possible values for specifying if the feature form element group
// * should be expanded or collapsed when the form is initially displayed.
// */
//public sealed class FeatureFormGroupState private constructor(internal val coreFeatureFormGroupState: CoreFeatureFormGroupState) {
//    /**
//     * The group element should be collapsed.
//     */
//    public object Collapsed : FeatureFormGroupState(CoreFeatureFormGroupState.COLLAPSED)
//
//    /**
//     * The group element should be expanded.
//     */
//    public object Expanded : FeatureFormGroupState(CoreFeatureFormGroupState.EXPANDED)
//
//    internal object Factory {
//        fun convertToPublic(coreFeatureFormGroupState: CoreFeatureFormGroupState): FeatureFormGroupState =
//            when (coreFeatureFormGroupState) {
//                CoreFeatureFormGroupState.COLLAPSED -> Collapsed
//                CoreFeatureFormGroupState.EXPANDED -> Expanded
//            }
//    }
//}
