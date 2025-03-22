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

package com.arcgismaps.toolkit.featureforms.internal.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

/**
 * Indicates the action to take when navigating.
 */
internal sealed class NavigationAction(val value: Int) {

    /**
     * Indicates no action.
     */
    data object None : NavigationAction(0)

    /**
     * Indicates an action to navigate back.
     */
    data object NavigateBack : NavigationAction(1)

    /**
     * Indicates an action to dismiss the form.
     */
    data object Dismiss : NavigationAction(2)

    /**
     * Indicates an action to navigate to an associated feature.
     */
    data object NavigateToAssociation : NavigationAction(3)

    /**
     * A [Saver] for [NavigationAction].
     */
    object NavigationActionSaver : Saver<NavigationAction, Int> {
        override fun restore(value: Int): NavigationAction {
            return when (value) {
                0 -> None
                1 -> NavigateBack
                2 -> Dismiss
                3 -> NavigateToAssociation
                else -> throw IllegalArgumentException("Unknown value: $value")
            }
        }

        override fun SaverScope.save(value: NavigationAction): Int {
            return value.value
        }
    }
}
