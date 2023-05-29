package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authenticationapp.screens.MainScreen
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            AuthenticationAppTheme {
                AuthenticationApp()
            }
        }
    }
}

@Composable
fun AuthenticationApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AuthenticationAppTheme {
        AuthenticationApp()
    }
}
