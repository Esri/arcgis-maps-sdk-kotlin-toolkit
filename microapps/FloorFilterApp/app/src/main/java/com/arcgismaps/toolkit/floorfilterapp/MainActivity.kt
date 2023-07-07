package com.arcgismaps.toolkit.floorfilterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.BuildConfig
import com.arcgismaps.toolkit.floorfilterapp.screens.MainScreen
import com.arcgismaps.toolkit.floorfilterapp.ui.theme.floorFilterAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            floorFilterAppTheme {
                floorFilterApp()
            }
        }
    }
}

@Composable
fun floorFilterApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun appPreview() {
    floorFilterAppTheme {
        floorFilterApp()
    }
}
