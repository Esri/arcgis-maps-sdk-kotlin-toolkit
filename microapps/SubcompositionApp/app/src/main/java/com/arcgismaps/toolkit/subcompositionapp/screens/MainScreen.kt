/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.subcompositionapp.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import com.arcgismaps.toolkit.subcomposition.Group
import com.arcgismaps.toolkit.subcomposition.runApp
import kotlinx.coroutines.awaitCancellation

@OptIn(InternalComposeApi::class)
@Composable
fun MainScreen() {
    val parentComposition = rememberCompositionContext()
    LaunchedEffect(Unit) {
        disposingComposition {
            com.arcgismaps.toolkit.subcomposition.runApp(parent = parentComposition)
        }
    }
    Text(text = "HELLO")
}


internal suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

