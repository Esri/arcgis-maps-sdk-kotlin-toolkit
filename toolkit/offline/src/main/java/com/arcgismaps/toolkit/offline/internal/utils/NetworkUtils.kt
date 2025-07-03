/*
 *
 *  Copyright 2025 Esri
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
package com.arcgismaps.toolkit.offline.internal.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import androidx.compose.runtime.State

@Composable
internal fun networkConnectivityState(): State<NetworkConnectionState> {
    val context = LocalContext.current

    // Creates a State<NetworkConnectionState> with current connectivity state as initial value
    return produceState(initialValue = context.currentConnectivityState) {
        // In a coroutine, can make suspend calls
        context.observeConnectivityAsFlow().collect { value = it }
    }
}

internal fun Context.observeConnectivityAsFlow() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = NetworkCallback(connectivityManager) { networkConnectionState -> trySend(networkConnectionState) }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(networkRequest, callback)

    // Set current state
    val currentState = getCurrentConnectivityState(connectivityManager)
    trySend(currentState)

    // Remove callback when not used
    awaitClose {
        // Remove listeners
        connectivityManager.unregisterNetworkCallback(callback)
    }
}

internal fun NetworkCallback(
    connectivityManager: ConnectivityManager,
    callback: (NetworkConnectionState) -> Unit
): ConnectivityManager.NetworkCallback {
    return object : ConnectivityManager.NetworkCallback() {
        /**
         * This callback is triggered when a network is available.
         * It indicates that the device has internet connectivity.
         */
        override fun onAvailable(network: Network) {
            callback(NetworkConnectionState.Available)
        }

        /**
         * This callback is triggered when a network is temporarily unavailable.
         * It does not necessarily mean that there is no internet connectivity,
         * but rather that the network is in a transient state (e.g., switching networks).
         */
        override fun onLost(network: Network) {
            // This callback is triggered when any previously available network is lost,
            // not just when all internet connectivity is lost. If the device switches
            // between networks (e.g., from Wi-Fi to mobile data), onLost is called for
            // the old network, even if another network is still available.
            val state = getCurrentConnectivityState(connectivityManager)
            callback(state)
        }
    }
}

/**
 * Network utility to get current state of internet connection
 */
internal val Context.currentConnectivityState: NetworkConnectionState
    get() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return getCurrentConnectivityState(connectivityManager)
    }

/**
 * Helper function to determine the current connectivity state.
 */
private fun getCurrentConnectivityState(
    connectivityManager: ConnectivityManager
): NetworkConnectionState {
    val connected = connectivityManager.allNetworks.any { network ->
        connectivityManager.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
    }

    return if (connected) NetworkConnectionState.Available else NetworkConnectionState.Unavailable
}

/**
 * Represents the state of network connectivity.
 *
 * - [Available]: Indicates that the device has internet connectivity.
 * - [Unavailable]: Indicates that the device does not have internet connectivity.
 */
internal sealed class NetworkConnectionState {
    object Available : NetworkConnectionState()
    object Unavailable : NetworkConnectionState()
}
