/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.mapviewinsetsapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.toolkit.geoviewcompose.MapView

/**
 * Shows how a composable [com.arcgismaps.toolkit.geoviewcompose.MapView] reacts to the
 * insets values specified on it. The left, right, top and bottom values can be specified
 * using the OutlinedTextFields. The attribution bar at the bottom of the MapView moves in
 * regards to the specified inset values. The `Reset Insets` button sets all inset values to zero.
 */
@Composable
fun MainScreen(modifier: Modifier) {
    val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }

    var insets by remember { mutableStateOf(PaddingValues()) }

    var leftText by remember { mutableStateOf(TextFieldValue("")) }
    var rightText by remember { mutableStateOf(TextFieldValue("")) }
    var topText by remember { mutableStateOf(TextFieldValue("")) }
    var bottomText by remember { mutableStateOf(TextFieldValue("")) }

    val updateInsets = remember {
        {
            insets = PaddingValues(
                leftText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                topText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                rightText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                bottomText.text.toDoubleOrNull()?.dp ?: 0.0.dp
            )
        }
    }

    val focusManager = LocalFocusManager.current
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            InsetTextField(
                value = leftText,
                onValueChange = { text: TextFieldValue -> leftText = text },
                label = { Text("Left") },
                updateInsets = updateInsets
            )

            InsetTextField(
                value = rightText,
                onValueChange = { text: TextFieldValue -> rightText = text },
                label = { Text("Right") },
                updateInsets = updateInsets
            )

            InsetTextField(
                value = topText,
                onValueChange = { text: TextFieldValue -> topText = text },
                label = { Text("Top") },
                updateInsets = updateInsets
            )

            InsetTextField(
                value = bottomText,
                onValueChange = { text: TextFieldValue -> bottomText = text },
                label = { Text("Bottom") },
                updateInsets = updateInsets
            )
        }

        Button(onClick = {
            insets = PaddingValues(0.0.dp, 0.0.dp, 0.0.dp, 0.0.dp)
            leftText = TextFieldValue("")
            rightText = TextFieldValue("")
            topText = TextFieldValue("")
            bottomText = TextFieldValue("")
            focusManager.clearFocus()
        }, Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
            Text("Reset Insets")
        }

        MapView(
            arcGISMap,
            modifier = Modifier.fillMaxSize(),
            insets = insets
        )
    }
}

@Composable
private fun InsetTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: @Composable () -> Unit,
    updateInsets: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        modifier = Modifier.size(90.dp, 64.dp),
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { updateInsets(); focusManager.clearFocus() })
    )
}
