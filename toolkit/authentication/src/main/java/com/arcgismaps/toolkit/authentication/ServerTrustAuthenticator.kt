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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType

/**
 * Displays a trust or distrust server prompt to the user in a Dialog.
 *
 * @param serverTrustChallenge the pending [ServerTrustChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this ServerTrustAuthenticator.
 * @since 200.6.0
 */
@Composable
internal fun ServerTrustAuthenticatorDialog(
    serverTrustChallenge: ServerTrustChallenge,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {
            serverTrustChallenge.distrust()
        }
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            ServerTrustAuthenticatorImpl(
                serverTrustChallenge.challenge.hostname,
                modifier,
                onConfirm = { serverTrustChallenge.trust() },
                onCancel = { serverTrustChallenge.distrust() }
            )
        }
    }
}

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param serverTrustChallenge the pending [ServerTrustChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this ServerTrustAuthenticator.
 * @since 200.2.0
 */
@Composable
internal fun ServerTrustAuthenticator(
    serverTrustChallenge: ServerTrustChallenge,
    modifier: Modifier = Modifier
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        ServerTrustAuthenticatorImpl(
            serverTrustChallenge.challenge.hostname,
            modifier,
            onConfirm = { serverTrustChallenge.trust() },
            onCancel = { serverTrustChallenge.distrust() }
        )
    }
}

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param hostname the hostname of the server to trust.
 * @param modifier the [Modifier] to be applied to this ServerTrustAuthenticator.
 * @param onConfirm the callback to be invoked when the user confirms the trust.
 * @param onCancel the callback to be invoked when the user cancels the trust.
 * @since 200.6.0
 */
@Composable
private fun ServerTrustAuthenticatorImpl(
    hostname: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.server_trust_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                val string = stringResource(
                    id = R.string.server_trust_message,
                    hostname
                )
                val startIdx = string.indexOf(hostname)
                val endIdx = startIdx + hostname.length
                append(string)
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = startIdx,
                    end = endIdx
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onCancel() }
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onConfirm() }) {
                Text(
                    text = stringResource(id = R.string.allow_connection),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun ServerTrustAuthenticatorImplPreview() {
    ServerTrustAuthenticatorImpl(
        "https://www.arcgis.com",
        onConfirm = {},
        onCancel = {}
    )
}

@Preview
@Composable
private fun ServerTrustAuthenticatorPreview() {
    val modifier = Modifier
    ServerTrustAuthenticator(
        serverTrustChallenge = ServerTrustChallenge(
            NetworkAuthenticationChallenge(
                "https://www.arcgis.com",
                NetworkAuthenticationType.ServerTrust,
                Throwable("Untrusted Host")
            )
        ) {},
        modifier = modifier
    )
}

@Preview
@Composable
private fun ServerTrustAuthenticatorDialogPreview() {
    val modifier = Modifier
    ServerTrustAuthenticatorDialog(
        serverTrustChallenge = ServerTrustChallenge(
            NetworkAuthenticationChallenge(
                "https://www.arcgis.com",
                NetworkAuthenticationType.ServerTrust,
                Throwable("Untrusted Host")
            )
        ) {},
        modifier = modifier
    )
}

@Preview(locale = "de") // Preview in German for long translations
@Composable
private fun ServerTrustAuthenticatorDialogLocalePreview() {
    ServerTrustAuthenticatorDialog(
        serverTrustChallenge = ServerTrustChallenge(
            NetworkAuthenticationChallenge(
                "https://www.arcgis.com",
                NetworkAuthenticationType.ServerTrust,
                Throwable("Untrusted Host")
            )
        ) {}
    )
}

