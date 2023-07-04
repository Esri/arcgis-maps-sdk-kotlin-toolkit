package com.arcgismaps.toolkit.authentication

import android.security.KeyChainAliasCallback

public data class CertificateChallenge(public val keyChainAliasCallback: KeyChainAliasCallback)