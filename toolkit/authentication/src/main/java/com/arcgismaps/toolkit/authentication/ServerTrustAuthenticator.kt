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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param serverTrustChallenge the pending [ServerTrustChallenge] that initiated this prompt.
 * @since 200.2.0
 */
@Composable
internal fun ServerTrustAuthenticator(
    serverTrustChallenge: ServerTrustChallenge
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = MaterialTheme.typography.headlineSmall.fontSize), startIdx, endIdx)

            } ,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.cancel),
                modifier = Modifier.clickable { serverTrustChallenge.distrust() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.allow_connection),
                modifier = Modifier.clickable { serverTrustChallenge.trust() },
                style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.End)
            )
        }
    }
}
