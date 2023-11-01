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

package com.arcgismaps.toolkit.mapinsetsapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.arcgismaps.toolkit.geocompose.MapView

@Composable
fun MainScreen() {
    val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            var mapInsets by remember { mutableStateOf(PaddingValues())}

            var leftText by remember { mutableStateOf(TextFieldValue("")) }
            var rightText by remember { mutableStateOf(TextFieldValue("")) }
            var topText by remember { mutableStateOf(TextFieldValue("")) }
            var bottomText by remember { mutableStateOf(TextFieldValue("")) }

            val updateInsets = {
                mapInsets = PaddingValues(
                    leftText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    topText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    rightText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    bottomText.text.toDoubleOrNull()?.dp ?: 0.0.dp
                )
            }

            val focusManager = LocalFocusManager.current
            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = arcGISMap,
                mapInsets = mapInsets
            )

            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedTextField(
                        value = leftText,
                        modifier = Modifier.size(90.dp, 64.dp),
                        onValueChange = { text: TextFieldValue -> leftText = text },
                        label = { Text("Left") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { updateInsets(); focusManager.clearFocus() })
                    )
                    OutlinedTextField(
                        value = rightText,
                        modifier = Modifier.size(90.dp, 64.dp),
                        onValueChange = { text: TextFieldValue -> rightText = text },
                        label = { Text("Right") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { updateInsets(); focusManager.clearFocus() })
                    )
                    OutlinedTextField(
                        value = topText,
                        modifier = Modifier.size(90.dp, 64.dp),
                        onValueChange = { text: TextFieldValue -> topText = text },
                        label = { Text("Top") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { updateInsets(); focusManager.clearFocus() })
                    )
                    OutlinedTextField(
                        value = bottomText,
                        modifier = Modifier.size(96.dp, 64.dp),
                        onValueChange = { text: TextFieldValue -> bottomText = text },
                        label = { Text("Bottom") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { updateInsets(); focusManager.clearFocus() })
                    )
                }
                Button(onClick = {
                    mapInsets = PaddingValues(0.0.dp, 0.0.dp, 0.0.dp, 0.0.dp)
                    leftText = TextFieldValue("")
                    rightText = TextFieldValue("")
                    topText = TextFieldValue("")
                    bottomText = TextFieldValue("")
                    focusManager.clearFocus()
                }, Modifier.fillMaxWidth()) {
                    Text("Reset Insets")
                }
            }
        }
    }
}
