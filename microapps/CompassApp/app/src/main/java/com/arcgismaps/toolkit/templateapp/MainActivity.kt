package com.arcgismaps.toolkit.compassapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.compassapp.screens.MainScreen
import com.arcgismaps.toolkit.compassapp.ui.theme.CompassAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            CompassAppTheme {
                CompassApp()
            }
        }
    }
}

@Composable
fun CompassApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    CompassAppTheme {
        CompassApp()
    }
}
