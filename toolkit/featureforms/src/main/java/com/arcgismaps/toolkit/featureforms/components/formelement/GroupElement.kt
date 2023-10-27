/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.GroupFormElement

@Composable
internal fun GroupElement(groupElement: GroupFormElement, form: FeatureForm) {
    val visible by groupElement.isVisible.collectAsState()
    if (visible) {
        GroupElement(label = groupElement.label, description = groupElement.description)
    }
}

@Composable
private fun GroupElement(modifier: Modifier = Modifier, label : String, description: String) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(5.dp)
            ),
    ) {
        GroupElementHeader(
            title = label,
            description = description,
            isRequired = true,
            isError = false
        )
    }
}

@Composable
private fun GroupElementHeader(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isRequired: Boolean,
    isError: Boolean
) {
    val label = remember(isRequired) {
        if (isRequired)
            "$title *"
        else title
    }
    Column(modifier = modifier
        .fillMaxWidth()
        .clickable { }
        .padding(15.dp)) {
        Text(text = label)
        Text(text = description)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF, showSystemUi = true)
@Composable
private fun GroupElementPreview() {
    GroupElement(label = "Title", description = "Description")
}
