/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.navigation

import android.os.Bundle
import androidx.navigation.NavType
import com.arcgismaps.portal.PortalFolder
import kotlinx.serialization.json.Json

/**
 * Custom NavType for [PortalFolder] to serialize and deserialize it.
 */
val PortalFolderNavType = object : NavType<PortalFolder>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): PortalFolder? = bundle.getString(key)?.let {
        parseValue(it)
    }

    override fun parseValue(value: String): PortalFolder = Json.decodeFromString(value)
    override fun serializeAsValue(value: PortalFolder) = Json.encodeToString(value)
    override fun put(bundle: Bundle, key: String, value: PortalFolder) {
        bundle.putString(key, serializeAsValue(value))
    }
}
