/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Default values for [BarcodeTextField].
 */
internal object BarcodeTextFieldDefaults {

    val barcodeIconTintColor: Color
        @Composable
        get() = if (isSystemInDarkTheme()) barcodeIconDarkTintColor else barcodeIconLightTintColor

    val barcodeIconSize = 24.dp

    private val barcodeIconLightTintColor = Color(0xFF1E88E5)
    private val barcodeIconDarkTintColor = Color(0xFF90CAF9)

}
