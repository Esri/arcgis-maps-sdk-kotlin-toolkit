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

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import com.arcgismaps.toolkit.geocompose.Map
import com.arcgismaps.toolkit.geocompose.MapInsets

@Composable
fun MainScreen() {
    val arcGISMap = remember { ArcGISMap(BasemapStyle.ArcGISImagery) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            var mapInsets by remember { mutableStateOf(WindowInsets(20.dp, 20.dp, 20.dp, 20.dp))}
//            var mapInsets by remember { mutableStateOf(MapInsets(20.dp, 20.dp, 20.dp, 20.dp))}
//            val mapInsets by remember { mutableStateOf(MapInsets(0.0, 0.0, 0.0, 0.0))}
//            var mapInsets by remember { mutableStateOf(PaddingValues(20.dp, 20.dp, 20.dp, 20.dp))}

            var leftText by remember { mutableStateOf(TextFieldValue("")) }
            var rightText by remember { mutableStateOf(TextFieldValue("")) }
            var topText by remember { mutableStateOf(TextFieldValue("")) }
            var bottomText by remember { mutableStateOf(TextFieldValue("")) }
            val updateInsets = {

//                mapInsets.start = leftText.text.toDoubleOrNull() ?: 0.0
//                Log.d("mapInset.start", mapInsets.start.toString())
//                mapInsets.end = rightText.text.toDoubleOrNull() ?: 0.0
//                mapInsets.top = topText.text.toDoubleOrNull() ?: 0.0
//                mapInsets.bottom = bottomText.text.toDoubleOrNull() ?: 0.0

//                mapInsets = MapInsets(leftText.text.toDoubleOrNull() ?: 0.0,
//                    rightText.text.toDoubleOrNull() ?: 0.0,
//                    topText.text.toDoubleOrNull() ?: 0.0,
//                    bottomText.text.toDoubleOrNull() ?: 0.0)

//                mapInsets = MapInsets(leftText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                    rightText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                    topText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                    bottomText.text.toDoubleOrNull()?.dp ?: 0.0.dp)
//            }

//            mapInsets = PaddingValues(leftText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                rightText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                topText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
//                bottomText.text.toDoubleOrNull()?.dp ?: 0.0.dp)

                mapInsets = WindowInsets(leftText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    rightText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    topText.text.toDoubleOrNull()?.dp ?: 0.0.dp,
                    bottomText.text.toDoubleOrNull()?.dp ?: 0.0.dp)
        }

            val focusManager = LocalFocusManager.current
            Map(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = arcGISMap,
                mapInsets = mapInsets
            )

            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedTextField(
                        value = leftText,
                        modifier = Modifier.size(90.dp, 64.dp),
                        onValueChange = { text: TextFieldValue -> leftText = text; Log.d("leftText", leftText.text)},
                        label = { Text("Left") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
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
//                    mapInsets.start = 0.0
//                    mapInsets.end = 0.0
//                    mapInsets.top = 0.0
//                    mapInsets.bottom = 0.0
//                    mapInsets = MapInsets(0.0.dp, 0.0.dp, 0.0.dp, 0.0.dp)
//                    mapInsets = PaddingValues(0.0.dp, 0.0.dp, 0.0.dp, 0.0.dp)
                    mapInsets = WindowInsets(0.0.dp, 0.0.dp, 0.0.dp, 0.0.dp)
                    leftText = TextFieldValue("")

                    focusManager.clearFocus()
                }, Modifier.fillMaxWidth()) {
                    Text("Reset Insets")
                }
            }
        }
    }
}
