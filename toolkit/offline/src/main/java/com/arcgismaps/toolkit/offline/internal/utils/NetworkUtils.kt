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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
// Add imports:
import android.util.Log
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.arcgismaps.toolkit.offline.workmanager.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

// Custom state saver for NetworkConnectionState
internal val NetworkConnectionStateSaver = Saver<NetworkConnectionState, Boolean>(
    save = { state -> state is NetworkConnectionState.Available },
    restore = { isAvailable ->
        if (isAvailable) NetworkConnectionState.Available else NetworkConnectionState.Unavailable
    }
)

@Composable
internal fun ObserveNetworkSwitching(onNetworkChanged: (NetworkConnectionState) -> Unit) {
    val context = LocalContext.current
    var previousState = rememberSaveable(saver = NetworkConnectionStateSaver) {
        context.currentConnectivityState
    }
    val currentOnNetworkChanged by rememberUpdatedState(onNetworkChanged)
    LaunchedEffect(Unit) {
        context.observeConnectivityAsFlow().collect { currentState ->
            Log.e(
                LOG_TAG,
                "ObserveNetworkSwitching: currentState: ${currentState.javaClass.simpleName}"
            )
            Log.e(
                LOG_TAG,
                "ObserveNetworkSwitching: previousState: ${previousState.javaClass.simpleName}"
            )
            if (previousState != currentState) {
                currentOnNetworkChanged(currentState)
                previousState = currentState
            }
        }
    }
}

private fun Context.observeConnectivityAsFlow() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var onLostJob: Job? = null
    val scope = CoroutineScope(Dispatchers.IO)
    val callback = NetworkCallback(
        connectivityManager = connectivityManager,
        scope = scope,
        onLost = { onLostJob = it },
        onCancelJob = { onLostJob?.cancel() }
    ) { networkConnectionState -> trySend(networkConnectionState) }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    Log.e(LOG_TAG, "Registering network callback.")
    connectivityManager.registerNetworkCallback(networkRequest, callback)

    // Unregister the callback when the flow is cancelled
    awaitClose {
        // Remove listeners
        Log.e(LOG_TAG, "Un-registering network callback.")
        connectivityManager.unregisterNetworkCallback(callback)
        onLostJob?.cancel()
    }
}.distinctUntilChanged() // Only emit when the state actually changes

private fun NetworkCallback(
    connectivityManager: ConnectivityManager,
    scope: CoroutineScope,
    onLost: (Job) -> Unit,
    onCancelJob: () -> Unit,
    callback: (NetworkConnectionState) -> Unit
): ConnectivityManager.NetworkCallback {
    // A delay in milliseconds to wait before confirming network loss.
    val networkLostDelayMs = 2000L
    return object : ConnectivityManager.NetworkCallback() {
        /**
         * This callback is triggered when a network is available.
         * It indicates that the device has internet connectivity.
         */
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            // A new network is available, so cancel any pending "lost" job.
            onCancelJob()
            val currentState = getCurrentConnectivityState(connectivityManager)
            Log.e(LOG_TAG, "onAvailable: currentState = ${currentState.javaClass.simpleName}")
            callback(currentState)
        }

        /**
         * This callback is triggered when a network is lost.
         * Awaits NETWORK_LOST_DELAY_MS before declaring unavailable.
         */
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.e(
                LOG_TAG,
                "Network lost: ${network.networkHandle}. Waiting for ${networkLostDelayMs}ms before declaring unavailable."
            )
            // A network was lost. Launch a delayed job to confirm it's not a temporary switch.
            onLost(scope.launch {
                delay(networkLostDelayMs)
                // If the job wasn't cancelled, it means no new network appeared in time.
                val currentState = getCurrentConnectivityState(connectivityManager)
                Log.e(LOG_TAG, "onLost: currentState = ${currentState.javaClass.simpleName}")
                callback(currentState)
            })
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
    val activeNetwork = connectivityManager.activeNetwork
    return if (activeNetwork != null) {
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkConnectionState.Available
        } else {
            NetworkConnectionState.Unavailable
        }
    } else {
        NetworkConnectionState.Unavailable
    }
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
