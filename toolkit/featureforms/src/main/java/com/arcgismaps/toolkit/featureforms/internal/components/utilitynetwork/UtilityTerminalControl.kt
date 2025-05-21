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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun UtilityTerminalControl(
    name : String,
    modifier: Modifier = Modifier
) {
    PropertyRow(
        title = stringResource(R.string.terminal),
        value = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun UtilityTerminalControlPreview() {
    val name = "SS"
    UtilityTerminalControl(
        name = name,
        modifier = Modifier.fillMaxWidth()
    )
}
