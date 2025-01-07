/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.scalebar

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
public fun Scalebar(viewModel: ScalebarInterface) {
    val text = viewModel.someProperty.collectAsState()
    Text(text = text.value)
}

@Preview
@Composable
internal fun ScalebarPreview() {
    val viewModel = object: ScalebarInterface {
        private val _someProperty: MutableStateFlow<String> = MutableStateFlow("Hello Scalebar Preview")
        override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
    }
    Scalebar(viewModel)
}
