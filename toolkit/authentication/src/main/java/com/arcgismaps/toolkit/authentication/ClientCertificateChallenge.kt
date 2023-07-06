package com.arcgismaps.toolkit.authentication

import android.security.KeyChain
import android.security.KeyChainAliasCallback

/**
 * Represents an authentication challenge requiring a client certificate.
 *
 * @property keyChainAliasCallback a [KeyChainAliasCallback] for calls to [KeyChain.choosePrivateKeyAlias].
 * @since 200.2.0
 */
public data class ClientCertificateChallenge(public val keyChainAliasCallback: KeyChainAliasCallback)
