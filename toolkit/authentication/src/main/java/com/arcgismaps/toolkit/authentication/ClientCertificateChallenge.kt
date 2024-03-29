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

import android.security.KeyChain
import android.security.KeyChainAliasCallback

/**
 * Represents an authentication challenge requiring a client certificate.
 *
 * @property keyChainAliasCallback a [KeyChainAliasCallback] for calls to [KeyChain.choosePrivateKeyAlias].
 * @property onCancel a lambda called when the challenge should be cancelled.
 * @since 200.2.0
 */
public data class ClientCertificateChallenge(
    public val keyChainAliasCallback: KeyChainAliasCallback,
    public val onCancel: () -> Unit
)
