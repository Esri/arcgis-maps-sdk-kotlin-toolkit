package com.arcgismaps.toolkit.featureformsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.toolkit.featureformsapp.screens.FeatureFormApp
import com.arcgismaps.toolkit.featureformsapp.ui.theme.FeatureFormsAppTheme
import dagger.hilt.android.AndroidEntryPoint

class FormsArcGISAuthenticationChallengeHandler(
    private val username: String,
    private val password: String
) : ArcGISAuthenticationChallengeHandler {
    override suspend fun handleArcGISAuthenticationChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val result: Result<TokenCredential> =
            TokenCredential.create(
                challenge.requestUrl,
                username,
                password,
                tokenExpirationInterval = 0
            )
        return result.let {
            if (it.isSuccess) {
                ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it.getOrThrow())
            } else {
                ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(it.exceptionOrNull()!!)
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            FormsArcGISAuthenticationChallengeHandler(
                BuildConfig.webMapUser,
                BuildConfig.webMapPassword
            )
        setContent {
            FeatureFormsAppTheme {
                FeatureFormApp()
            }
        }
    }
}

