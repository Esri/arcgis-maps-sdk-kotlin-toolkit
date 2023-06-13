package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays a trust or distrust server prompt to the user.
 *
 * @param onTrust called when the trust button is clicked.
 * @param onDistrust called when the cancel button is clicked.
 * @param challengeHostname the hostname of the challenge this prompt is associated with.
 * @since 200.2.0
 */
@Composable
internal fun ServerTrustAuthenticator(
    onTrust: () -> Unit,
    onDistrust: () -> Unit,
    challengeHostname: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "The certificate provided by $challengeHostname is not signed by a trusted authority.",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cancel",
                modifier = Modifier.clickable { onDistrust() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Dangerous: Allow Connection",
                modifier = Modifier.clickable { onTrust() },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}