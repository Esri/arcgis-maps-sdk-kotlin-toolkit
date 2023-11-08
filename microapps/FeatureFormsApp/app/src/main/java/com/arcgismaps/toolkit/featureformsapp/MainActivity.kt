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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatureFormsAppTheme {
                FeatureFormApp()
            }
        }
    }
}
