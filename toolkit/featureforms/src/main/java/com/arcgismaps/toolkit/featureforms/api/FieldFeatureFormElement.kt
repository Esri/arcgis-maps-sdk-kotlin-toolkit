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

package com.arcgismaps.toolkit.featureforms.api

import com.arcgismaps.data.Domain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer


/**
 * Defines how a field in the dataset participates in the form.
 */
@SerialName("field")
@Serializable
public class FieldFeatureFormElement internal constructor(
    override var description: String,
    override var label: String,
    override var visibilityExpressionName: String = "",
    // No way to construct a Domain without core.
//    @Serializable(with = DomainSerializer::class)
//    var domain: Domain,
    public var editableExpressionName: String ="",
    public var fieldName: String,
    public val inputType: FeatureFormInput,
    public val value: String = "",
    public var requiredExpressionName: String = "",
    public var valueExpressionName: String = ""
    ) : FeatureFormElement {
    //region Properties
    
    /**
     * The domain to apply to this field. If defined, it takes precedence over domains
     * defined in field, type, or subtype.
     */
    
    
    /**
     * A reference to an Arcade expression that returns a boolean value. When this expression
     * evaluates to true, the element is editable. When the expression evaluates to false
     * the element is not editable. If the referenced field is not editable, the editable
     * expression is ignored and the element is not editable.
     */
 
    
    /**
     * A string containing the field name as defined by the feature layer.
     */
 
    /**
     * The value for the field specified by [FieldFeatureFormElement.fieldName].
     * This property is populated when [FeatureForm.evaluateExpressionsAsync()] is called.
     * Dates and times are returned in the local timezone.
     *
     * If a field is part of a [CodedValueDomain], the [CodedValue.name] is returned.
     *
     * If [PopupElement.isEvaluated] is false, this property will return an empty collection.
     */
    
    
    /**
     * The input user interface to use for the element. If an input type is not supplied
     * or the client application does not understand the supplied input type, the client
     * application is responsible for defining the default user interface.
     */
    
    
    /**
     * A reference to an Arcade expression that returns a boolean value. When this
     * expression evaluates to true and the element is visible, the element must
     * have a valid value in order for the feature to be created or edited. When
     * the expression evaluates to false the element is not required. If no expression
     * is provided, the default behavior is that the element is not required. If the
     * referenced field is non-nullable, the required expression is ignored and the
     * element is always required.
     */
    
    
    /**
     * A reference to an Arcade expression that returns a date, number, or string value.
     * When this expression evaluates the value of the field will be updated to the result.
     * This expression is only evaluated when editableExpression (if defined) or editable
     * is false. If the referenced field is not editable, the expression is ignored.
     */
    
    
    
    //endregion Factories
    
}


internal object DomainStringSerializer : KSerializer<Domain> {
    override fun deserialize(decoder: Decoder): Domain {
        return Domain.fromJsonOrNull(decoder.decodeString())!!
    }
    
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Domain", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Domain) =
        encoder.encodeString(value.toJson())
}

internal object DomainSerializer :
    JsonTransformingSerializer<Domain>(tSerializer = DomainStringSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(value = element.toString())
    }
}
