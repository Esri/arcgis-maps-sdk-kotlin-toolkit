package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.AuthenticationManager
import com.arcgismaps.httpcore.authentication.OAuthUserCredential

/**
 * Revokes OAuth tokens and removes all credentials from the [AuthenticationManager.arcGISCredentialStore]
 * and [AuthenticationManager.networkCredentialStore].
 *
 * @since 200.2.0
 */
public suspend fun AuthenticationManager.signOut() {
    arcGISCredentialStore.getCredentials().forEach {
        if (it is OAuthUserCredential) {
            it.revokeToken()
        }
    }
    arcGISCredentialStore.removeAll()
    networkCredentialStore.removeAll()
}
