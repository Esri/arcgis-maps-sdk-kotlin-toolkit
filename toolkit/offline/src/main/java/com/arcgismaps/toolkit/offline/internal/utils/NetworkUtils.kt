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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.toolkit.offline.workmanager.LOG_TAG
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Custom state saver for NetworkConnectionState
internal val NetworkConnectionStateSaver = Saver<NetworkConnectionState, Boolean>(
    save = { state -> state is NetworkConnectionState.Available },
    restore = { isAvailable ->
        if (isAvailable) NetworkConnectionState.Available else NetworkConnectionState.Unavailable
    }
)

@Composable
internal fun ObserveNetworkSwitching(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onNetworkChanged: (NetworkConnectionState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnNetworkChanged by rememberUpdatedState(onNetworkChanged)
    var previousNetworkState = rememberSaveable(saver = NetworkConnectionStateSaver) {
        context.currentConnectivityState
    }
    DisposableEffect(lifecycleOwner) {
        // Lifecycle observer to handle lifecycle events
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Update the network status on resume
                val currentNetworkState = context.currentConnectivityState
                if (previousNetworkState != currentNetworkState) {
                    Log.e(
                        LOG_TAG,
                        "ON_RESUME: previousState: ${previousNetworkState.javaClass.simpleName}"
                    )
                    Log.e(
                        LOG_TAG,
                        "ON_RESUME: currentState: ${currentNetworkState.javaClass.simpleName}"
                    )
                    previousNetworkState = currentNetworkState
                    currentOnNetworkChanged(previousNetworkState)
                }
            }
        }
        scope.launch {
            context.observeConnectivityAsFlow().collect { currentNetworkState ->
                if (previousNetworkState != currentNetworkState) {
                    Log.e(
                        LOG_TAG,
                        "ObserveNetworkSwitching: previousState: ${previousNetworkState.javaClass.simpleName}"
                    )
                    Log.e(
                        LOG_TAG,
                        "ObserveNetworkSwitching: currentState: ${currentNetworkState.javaClass.simpleName}"
                    )
                    previousNetworkState = currentNetworkState
                    currentOnNetworkChanged(previousNetworkState)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun Context.observeConnectivityAsFlow() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = NetworkCallback(connectivityManager) { networkConnectionState ->
        trySend(networkConnectionState)
    }

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
    }
}.distinctUntilChanged() // Only emit when the state actually changes

private fun NetworkCallback(
    connectivityManager: ConnectivityManager,
    callback: (NetworkConnectionState) -> Unit
): ConnectivityManager.NetworkCallback {
    return object : ConnectivityManager.NetworkCallback() {
        /**
         * This callback is triggered when a network is available.
         * It indicates that the device has internet connectivity.
         */
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val currentState = getNetworkState(network, connectivityManager)
            Log.e(LOG_TAG, "onAvailable: currentState = ${currentState.javaClass.simpleName}")
            callback(currentState)
        }

        /**
         * This callback is triggered when a network is lost.
         */
        override fun onLost(network: Network) {
            super.onLost(network)
            val currentState = getNetworkState(network, connectivityManager)
            Log.e(LOG_TAG, "onLost: currentState = ${currentState.javaClass.simpleName}")
            callback(currentState)
        }


        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val currentState = getNetworkState(network, connectivityManager)
            Log.e(
                LOG_TAG,
                "onCapabilitiesChanged: currentState = ${currentState.javaClass.simpleName}"
            )
            callback(currentState)
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
        getNetworkState(activeNetwork, connectivityManager)
    } else {
        NetworkConnectionState.Unavailable
    }
}

private fun getNetworkState(
    network: Network,
    connectivityManager: ConnectivityManager
): NetworkConnectionState {
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return if (capabilities != null &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    ) {
        NetworkConnectionState.Available
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
