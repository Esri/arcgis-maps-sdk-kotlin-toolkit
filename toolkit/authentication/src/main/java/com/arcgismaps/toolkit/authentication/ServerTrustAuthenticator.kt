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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param serverTrustChallenge the pending [ServerTrustChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this ServerTrustAuthenticator.
 * @since 200.2.0
 */
@Composable
internal fun ServerTrustAuthenticatorDialog_NourVersion(
    serverTrustChallenge: ServerTrustChallenge,
    modifier: Modifier = Modifier
) {
    AlertDialog(modifier = modifier, text = {
        Text(
            text = buildAnnotatedString {
                val hostname = serverTrustChallenge.challenge.hostname
                val string = stringResource(
                    id = R.string.server_trust_message,
                    hostname
                )
                val startIdx = string.indexOf(hostname)
                val endIdx = startIdx + hostname.length
                append(string)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                    ), startIdx, endIdx
                )
            },
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start
        )
    }, onDismissRequest = { serverTrustChallenge.distrust() }, confirmButton = {
        TextButton(onClick = { serverTrustChallenge.trust() }) {
            Text(text = stringResource(id = R.string.allow_connection))
        }
    }, dismissButton = {
        TextButton(onClick = { serverTrustChallenge.distrust() }) {
            Text(stringResource(id = R.string.cancel))
        }
    })
}

@Composable
internal fun ServerTrustAuthenticatorDialog_Erick(
    serverTrustChallenge: ServerTrustChallenge,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {
            serverTrustChallenge.distrust()
        }
    ) {
        ServerTrustAuthenticator(
            serverTrustChallenge.challenge.hostname,
            modifier,
            onConfirm = { serverTrustChallenge.trust() },
            onCancel = { serverTrustChallenge.distrust() }
        )
    }
}

@Composable
private fun ServerTrustAuthenticator(
    hostName: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = modifier
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                text = buildAnnotatedString {
                    val string = stringResource(
                        id = R.string.server_trust_message,
                        hostName
                    )
                    val startIdx = string.indexOf(hostName)
                    val endIdx = startIdx + hostName.length
                    append(string)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        ), startIdx, endIdx
                    )
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onCancel() }
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = {
                    onConfirm()
                }) {
                    Text(
                        modifier = Modifier.padding(0.dp),
                        text = stringResource(id = R.string.allow_connection),
                        style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center)

                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun ServerTrustAuthenticator_Erick_Preview() {
    val modifier = Modifier
    Surface(modifier = modifier.fillMaxSize()) {
        ServerTrustAuthenticator("https://www.arcgis.com", modifier = modifier, {}, {})
    }
}

@Preview
@Composable
private fun ServerTrustAuthenticatorDialog_Erick_Preview() {
    val modifier = Modifier/*.fillMaxSize()*/
//        .background(Color.Green)
//        .padding(16.dp)
    ServerTrustAuthenticatorDialog_Erick(
        serverTrustChallenge = ServerTrustChallenge(
            NetworkAuthenticationChallenge(
                "https://www.arcgis.com",
                NetworkAuthenticationType.ServerTrust,
                Throwable()
            ), {}), modifier
    )
}

@Preview(locale = "hu")
@Composable
private fun ServerTrustAuthenticatorDialog_Erick_Preview_LONG() {
    Dialog(onDismissRequest = {}) {
        ServerTrustAuthenticator("https://www.arcgis.com", modifier = Modifier, {}, {})
    }
}
