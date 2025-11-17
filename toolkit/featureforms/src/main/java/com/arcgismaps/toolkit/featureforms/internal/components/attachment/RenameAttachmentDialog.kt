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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R

@Composable
internal fun RenameAttachmentDialog(
    name: String,
    onRename: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val groups = rememberSaveable(name) { name.split("\\.(?=[^\\\\.]+\$)".toRegex()) }
    var filename by rememberSaveable(groups) { mutableStateOf(groups.first()) }
    val extension = rememberSaveable(groups) { if (groups.count() == 2) groups.last() else "" }
    val focusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        content = {
            Surface (
                modifier = Modifier.wrapContentSize(),
                shape = RoundedCornerShape(25.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.rename_attachment),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    TextField(
                        value = TextFieldValue(
                            text = filename,
                            selection = TextRange(filename.length)
                        ),
                        onValueChange = { value -> filename = value.text },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                focusRequester.requestFocus()
                            },
                        label = { Text(stringResource(R.string.name)) },
                        suffix = {
                            Text(text = ".$extension")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                        )
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                        TextButton(
                            onClick = { onRename("$filename.$extension") },
                            enabled = filename.isNotEmpty()
                        ) {
                            Text(text = stringResource(id = R.string.rename))
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun RenameAttachmentDialogPreview() {
    RenameAttachmentDialog(
        name = "Photo 1.jpg",
        onRename = {},
        onDismissRequest = {}
    )
}
