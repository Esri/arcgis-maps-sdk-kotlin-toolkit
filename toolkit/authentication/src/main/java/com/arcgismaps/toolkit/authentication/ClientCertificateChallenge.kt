package com.arcgismaps.toolkit.authentication

import android.security.KeyChainAliasCallback

public data class ClientCertificateChallenge(public val keyChainAliasCallback: KeyChainAliasCallback)
