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

package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param serverTrustChallenge the pending [ServerTrustChallenge] that initiated this prompt.
 * @since 200.2.0
 */
@Composable
internal fun ServerTrustAuthenticator(
    serverTrustChallenge: ServerTrustChallenge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.92f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Text(text = "Untrusted Host")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hostname = serverTrustChallenge.challenge.hostname
                    Text(
                        text = "The certificate provided by $hostname is not signed by a trusted authority.",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Start
                    )
                }
            },
            onDismissRequest = { serverTrustChallenge.distrust() },
            confirmButton = {
                TextButton(
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = Color.Transparent
                    ),
                    onClick = { serverTrustChallenge.trust() }) {
                    Text(text = "Allow Connection")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { serverTrustChallenge.distrust() }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
