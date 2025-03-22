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

package com.arcgismaps.toolkit.featureforms.internal.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.utilitynetworks.UtilityAssociationGroupResult
import com.arcgismaps.utilitynetworks.UtilityAssociationsFilterResult

/**
 * Composable function that displays the given [filterResult].
 *
 * @param filterResult The filter result.
 * @param onGroupSelected The callback that is invoked when a group is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun UNAssociationsFilterScreen(
    filterResult: UtilityAssociationsFilterResult,
    onGroupSelected: (UtilityAssociationGroupResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        UtilityAssociationFilter(
            groupResults = filterResult.groupResults,
            onGroupClick = onGroupSelected,
            modifier = Modifier
                .padding(16.dp)
                .wrapContentSize()
        )
    }
}
