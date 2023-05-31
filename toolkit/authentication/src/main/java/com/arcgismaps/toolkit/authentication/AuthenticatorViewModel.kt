package com.arcgismaps.toolkit.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 * This should be set as the [ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler]
 * and [ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler].
 *
 * @since 200.2.0
 */
public class AuthenticatorViewModel : ViewModel(), NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler {

    private val _shouldShowDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    /**
     * Whether an alert dialog should be displayed to the user. For initial testing purposes only,
     * this will be removed later.
     *
     * @since 200.2.0
     */
    public val shouldShowDialog: StateFlow<Boolean> = _shouldShowDialog.asStateFlow()

    init {
        viewModelScope.launch {
            delay(10_000)
            _shouldShowDialog.emit(true)
        }
    }

    /**
     * Instructs the viewModel to dismiss the alert dialog. For initial testing purposes only,
     * this will be removed later.
     *
     * @since 200.2.0
     */
    public fun dismissDialog(): Boolean = _shouldShowDialog.tryEmit(false)

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }

}
