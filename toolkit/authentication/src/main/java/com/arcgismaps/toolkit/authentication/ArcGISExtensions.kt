package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.AuthenticationManager
import com.arcgismaps.httpcore.authentication.OAuthUserCredential

public suspend fun AuthenticationManager.signOut() {
    arcGISCredentialStore.getCredentials().forEach {
        if (it is OAuthUserCredential) {
            it.revokeToken()
        }
    }
    arcGISCredentialStore.removeAll()
    networkCredentialStore.removeAll()
}