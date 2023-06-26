package com.arcgismaps.toolkit.featureforms.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * An interface that provides a String which can be decoded.
 *
 * @since 200.0.0
 */
public interface StringEncodable {
    public val encoding: String
}

/**
 * An interface which indicates that the deserialization of this type supports the capture of
 * any String into an "unknown" sealed class subtype.
 *
 * @since 200.0.0
 */
public interface UnknownStringEncodable : StringEncodable

/**
 * A class to codify the creation of an object from a String. In practice this class is a
 * companion object of a sealed class. The sealed class represents an Enum type in the Maps SDK API.
 * The set of Enum value is finite. For support of unknown Enum values, use
 * [SealedClassStringDecoderWithUnknownEncoding].
 *
 * @since 200.0.0
 */
internal abstract class SealedClassStringDecoder<T : StringEncodable> {
    /**
     * A mapping from String to singleton subclasses of [T]. This mapping makes use of
     * [sealedClassMapNotNull], which is a reified inline function, and cannot be invoked
     * polymorphically. So it is up to the individual instantiations of this class to call it.
     *
     * @since 200.0.0
     */
    abstract val decoder: Map<String, T>
    
    /**
     * Create an instance of [T] from a String
     *
     * @since 200.0.0
     */
    open fun decode(encoding: String): T = decoder[encoding]
        ?: throw SerializationException("Unknown string encountered: $encoding")
}

/**
 * A subclass of [SealedClassStringDecoder] to capture unknown encoded values into an Unknown
 * sealed class subtype.
 *
 * @since 200.0.0
 */
internal abstract class SealedClassStringDecoderWithUnknownEncoding<T : UnknownStringEncodable> :
    SealedClassStringDecoder<T>() {
    /**
     * A method from which to instantiate an "unknown" subtype of the base sealed class [T].
     *
     * @param unknownEncoding the unknown string encountered during json deserialization
     * @return an instance of the base sealed class, which should be an "unknown" subclass
     * @see [com.arcgismaps.portal.FeatureFormGroupState.Unknown]
     * @since 200.0.0
     */
    abstract fun unknownCase(unknownEncoding: String): T
    
    override fun decode(encoding: String): T = decoder[encoding] ?: unknownCase(encoding)
}

/**
 * A [KSerializer] which relies on a [SealedClassStringDecoder] to
 * decode a String into an object. A class that uses this class for serialization
 * would pass a singleton `object` subclass to a [JsonTransformingSerializer].
 *
 * @see StringTransformingSerializer
 * @since 200.0.0
 */
internal open class SealedClassStringSerializer<T : StringEncodable>(
    private val sealedClassDecoder: SealedClassStringDecoder<T>
) : KSerializer<T> {
    override fun deserialize(decoder: Decoder): T =
        sealedClassDecoder.decode(decoder.decodeString())
    
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(sealedClassDecoder::javaClass.name, PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: T): Unit =
        encoder.encodeString(value.encoding)
}

/**
 * We use sealed classes to represent enums in the API Specification, which are serialized as Strings
 * in the Sharing REST API. Since sealed classes are first class objects, the serialization decoder
 * needs to be "tamed" to expect simple Strings, as opposed to a structure in the encoded String.
 *
 * This is accomplished using two small (de)serialization objects:
 * * This JsonTransformingSerializer, which when it encounters a key indicating a sealed class,
 * transforms the JsonObject to a String, and strips surrounding quotes.
 * * a "primitive" serializer, which converts the String passed from the transforming serializer to
 * an instance of a sealed class.
 *
 * @since 200.0.0
 */
internal open class StringTransformingSerializer<T : StringEncodable>(serializer: SealedClassStringSerializer<T>) :
    JsonTransformingSerializer<T>(serializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(element.toString().droppingSurroundingText)
    }
}

/**
 * Drop leading and trailing non alphanumeric characters.
 * In practice, converting an encountered JsonObject, such as a sealed subclass encoded as a String,
 * and transforming it to a String will include any quotation marks with the String.
 */
internal val String.droppingSurroundingText: String
    get() {
        return this
            .dropWhile { !it.isLetterOrDigit() }
            .dropLastWhile { !it.isLetterOrDigit() }
    }

/**
 * A method to iterate and map over all the object singleton subclasses of a sealed class.
 *
 * @since 200.0.0
 */
internal inline fun <reified T, S> sealedClassMapNotNull(action: (T) -> S?): Iterable<S> =
    T::class.sealedSubclasses.mapNotNull { kClass ->
        kClass.objectInstance?.let {
            action(it)
        }
    }.asIterable()
