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
