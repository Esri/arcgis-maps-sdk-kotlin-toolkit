/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.ui.expandablecard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * State object that can be hoisted to control the [ExpandableCard].
 *
 * In most cases, this will be created using [rememberExpandableCardState].
 * @since 200.6.0
 */
internal class ExpandableCardState internal constructor(initialExpandedState: Boolean) {
    /**
     * The expanded state of the [ExpandableCard].
     * @since 200.6.0
     */
    internal var isExpanded by mutableStateOf(initialExpandedState)
        private set
    
    /**
     * Toggles the expanded state of the [ExpandableCard].
     *
     * @since 200.6.0
     */
    fun toggle() {
        isExpanded = !isExpanded
    }
}

/**
 * Remember the state of [ExpandableCard].
 *
 * @param isExpanded the initial expanded state
 * @since 200.6.0
 */
@Composable
internal fun rememberExpandableCardState(
    isExpanded: Boolean = true
): ExpandableCardState {
    return rememberSaveable(
        saver = Saver(
            save = { it.isExpanded},
            restore = { ExpandableCardState(it) }
        )
    ) {
        ExpandableCardState(isExpanded)
    }
}
