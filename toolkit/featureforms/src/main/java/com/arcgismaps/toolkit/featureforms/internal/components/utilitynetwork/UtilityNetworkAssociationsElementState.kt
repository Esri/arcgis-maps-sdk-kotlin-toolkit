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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import com.arcgismaps.utilitynetworks.UtilityAssociation
import com.arcgismaps.utilitynetworks.UtilityAssociationType
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class UtilityNetworkAssociationsElementState(
    id : Int,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    utilityNetwork: UtilityNetwork?,
    utilityElement: UtilityElement?,
    scope: CoroutineScope
) : FormElementState(
    id = id,
    label = label,
    description = description,
    isVisible = isVisible
) {
    var associations : MutableState<Map<UtilityAssociationType, List<UtilityAssociation>>> = mutableStateOf(
        emptyMap()
    )
        private set

    var selectedAssociationType : UtilityAssociationType? = null

    init {
        scope.launch {
            if (utilityElement != null) {
                utilityNetwork?.getAssociations(utilityElement)?.onSuccess { res ->
                    associations.value = res.groupBy { it.associationType }
                }
            }
        }
    }
}
