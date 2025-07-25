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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Indicates the action to take when navigating.
 */
@Parcelize
internal sealed class NavigationAction(val value: Int) : Parcelable {

    /**
     * Indicates no action.
     */
    @Parcelize
    data object None : NavigationAction(0)

    /**
     * Indicates an action to navigate back.
     */
    @Parcelize
    data object NavigateBack : NavigationAction(1)

    /**
     * Indicates an action to dismiss the form.
     */
    @Parcelize
    data object Dismiss : NavigationAction(2)

    /**
     * Indicates an action to navigate to an associated feature.
     *
     * @param index The index of the association to navigate to.
     */
    @Parcelize
    data class NavigateToFeature(val index : Int) : NavigationAction(3)
}
