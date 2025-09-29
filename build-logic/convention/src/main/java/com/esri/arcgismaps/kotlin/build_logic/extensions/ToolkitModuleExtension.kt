package com.esri.arcgismaps.kotlin.build_logic.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ToolkitModuleExtension @Inject constructor(objects: ObjectFactory) {
    /** Can/should this module be published as an artifact? */
    val releasable: Property<Boolean> = objects.property(Boolean::class.java)

    internal fun applyDefaults() {
        releasable.convention(true)
    }

    companion object {
        const val NAME = "toolkit"
    }
}
