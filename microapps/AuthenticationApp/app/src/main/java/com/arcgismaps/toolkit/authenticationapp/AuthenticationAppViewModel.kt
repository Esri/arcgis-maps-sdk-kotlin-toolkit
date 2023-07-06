package com.arcgismaps.toolkit.authenticationapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.signOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// Temp Test Data
private const val DEFAULT_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
const val arcgisPortal = "https://www.arcgis.com"
val arcgisConf = OAuthUserConfiguration(arcgisPortal, "QrpBAoS7KccFerE3","my-ags-app://auth")
const val samlPortal = "https://rt-saml1.esri.com/portal"
val samlConfig = OAuthUserConfiguration(samlPortal, "Ttj5gUYkSXxJVNjv", /*"samlapp://auth"*/DEFAULT_REDIRECT_URI)
const val selfSignedPortal = "https://rt-server107a.esri.com/portal"
val selfSignedConfig = OAuthUserConfiguration(selfSignedPortal, "1BADxtERjogAQG4u", DEFAULT_REDIRECT_URI)


class AuthenticationAppViewModel(application: Application) : AndroidViewModel(application) {

    val authenticatorState: AuthenticatorState = AuthenticatorState()

    private val noPortalInfoText = application.getString(R.string.no_portal_info)
    private val startInfoText = application.getString(R.string.start_info_text)
    private val arcGISUrl = "https://www.arcgis.com"
    private val oAuthUserConfiguration = samlConfig/*OAuthUserConfiguration(
        arcGISUrl,
        // This client ID is for demo purposes only. For use of the Authenticator in your own app,
        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
        "lgAdHkYZYlwwfAhC",
        "my-ags-app://auth"
    )*/

    private val _infoText: MutableStateFlow<String> = MutableStateFlow(startInfoText)
    val infoText: StateFlow<String> = _infoText

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _useOAuth: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val useOAuth: StateFlow<Boolean> = _useOAuth.asStateFlow()
    fun setUseOAuth(shouldUseOAuth: Boolean) { _useOAuth.value = shouldUseOAuth }

    private val _url: MutableStateFlow<String> = MutableStateFlow(oAuthUserConfiguration.portalUrl)
    val url: StateFlow<String> = _url.asStateFlow()
    fun setUrl(newUrl: String) { _url.value = newUrl }

    fun signOut() = viewModelScope.launch {
        _isLoading.value = true
        ArcGISEnvironment.authenticationManager.signOut()
        _infoText.value = startInfoText
        _isLoading.value = false
    }

    public fun loadPortal() = viewModelScope.launch {
        _isLoading.value = true
        authenticatorState.oAuthUserConfiguration =
            if (useOAuth.value) oAuthUserConfiguration else null
        val portal = Portal(url.value, Portal.Connection.Authenticated)
        portal.load().also {
            _isLoading.value = false
        }.onFailure {
            _infoText.value = it.toString()
        }.onSuccess {
            val text = portal.portalInfo?.let {
                val json = it.toJson()
                val jsonObject = JSONObject(json)
                jsonObject.toString(4)
            } ?: noPortalInfoText
            _infoText.value = text
        }
    }
}
