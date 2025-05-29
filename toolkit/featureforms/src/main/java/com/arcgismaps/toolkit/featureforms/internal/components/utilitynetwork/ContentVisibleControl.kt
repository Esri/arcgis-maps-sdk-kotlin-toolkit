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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityAssociation

/**
 * A composable that represents a content visible control of a [UtilityAssociation], specifically the
 * [UtilityAssociation.isContainmentVisible] property.
 *
 * @param value The current value of the content visible control.
 * @param enabled A boolean indicating whether the control is enabled or not.
 * @param onValueChange A callback that is called when the value of the control changes.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun ContentVisibleControl(
    value : Boolean,
    enabled : Boolean,
    onValueChange : (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = stringResource(R.string.containment_visible))
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
            enabled = enabled,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentVisibleControlPreview() {
    ContentVisibleControl(
        value = true,
        onValueChange = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth()
    )
}
