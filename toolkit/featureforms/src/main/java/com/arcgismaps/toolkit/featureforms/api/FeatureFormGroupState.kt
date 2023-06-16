package com.arcgismaps.toolkit.featureforms.api

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
 * The list of possible values for specifying if the feature form element group
 * should be expanded or collapsed when the form is initially displayed.
 */
@Serializable(with = FeatureFormGroupStateSerializer::class)
public sealed class FeatureFormGroupState(override val encoding: String): StringEncodable {
    /**
     * The group element should be collapsed.
     */
    public object Collapsed : FeatureFormGroupState("collapsed")
    
    /**
     * The group element should be expanded.
     */
    public object Expanded : FeatureFormGroupState("expanded")
    
    internal companion object :
        SealedClassStringDecoder<FeatureFormGroupState>() {
        override val decoder: Map<String, FeatureFormGroupState>
            get() = sealedClassMapNotNull<FeatureFormGroupState, Pair<String, FeatureFormGroupState>> { testSealedClass ->
                testSealedClass.encoding to testSealedClass
            }.toMap()
    }
}

internal object FeatureFormGroupStateSerializer :
    StringTransformingSerializer<FeatureFormGroupState>(
        FeatureFormGroupStateStringSerializer
    )

/**
 * A String serializer which decodes Strings into instances of [FeatureFormGroupState]. This serializer
 * is called from [FeatureFormGroupStateSerializer].
 *
 * @since 200.0.0
 */
internal object FeatureFormGroupStateStringSerializer :
    SealedClassStringSerializer<FeatureFormGroupState>(FeatureFormGroupState.Companion)
