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

internal data class UtilityAssociationGroup(
    val type: UtilityAssociationType,
    val elements: Map<String, List<UtilityAssociation>>
) {
    val count = elements.values.sumBy { it.size }

    fun getLayers(): List<String> {
        return elements.keys.toList()
    }
}

internal class UtilityNetworkAssociationsElementState(
    id: Int,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    utilityNetwork: UtilityNetwork?,
    val utilityElement: UtilityElement?,
    scope: CoroutineScope
) : FormElementState(
    id = id,
    label = label,
    description = description,
    isVisible = isVisible
) {
    var groups: MutableState<List<UtilityAssociationGroup>> = mutableStateOf(emptyList())
        private set

    val source : String = (utilityElement?.objectId ?: "").toString()

    init {
        scope.launch {
            if (utilityElement != null) {
                utilityNetwork?.getAssociations(utilityElement)?.onSuccess { res ->
                    groups.value = buildList {
                        // Group by association type
                        val typeMap = res.groupBy { it.associationType }
                        typeMap.keys.forEach { type ->
                            // for each type, group by network source or layer
                            val x = typeMap[type] ?: emptyList()
                            val groups = x.groupBy {
                                it.getTargetElement(utilityElement).networkSource.name
                            }
                            add(UtilityAssociationGroup(type, groups))
                        }
                    }
                }
            }
        }
    }

//    fun getLayerGroups(type: UtilityAssociationType): List<String> {
//        return associations.value[type]?.map {
//            it.getTargetElement(utilityElement!!).networkSource.name
//        } ?: emptyList()
//    }
//
//    fun getAssociations(layer: String, type: UtilityAssociationType): List<UtilityAssociation> {
//        return associations.value[type]?.filter {
//            it.getTargetElement(utilityElement!!).networkSource.name == layer
//        } ?: emptyList()
//    }
}

internal val UtilityAssociationType.name: String
    get() {
        val input = this.toString()
        val regex = Regex("UtilityAssociationType\\$(\\w+)@")
        val matchResult = regex.find(input)
        return matchResult?.groupValues?.get(1) ?: input
    }

internal fun UtilityAssociation.getTargetElement(element: UtilityElement): UtilityElement {
    return if (element.objectId == this.fromElement.objectId) {
        this.toElement
    } else {
        this.fromElement
    }
}
