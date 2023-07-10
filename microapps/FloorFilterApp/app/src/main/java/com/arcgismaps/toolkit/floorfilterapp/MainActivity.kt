package com.arcgismaps.toolkit.floorfilterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.floorfilterapp.screens.MainScreen
import com.arcgismaps.toolkit.floorfilterapp.ui.theme.FloorFilterAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            FloorFilterAppTheme {
                FloorFilterApp()
            }
        }
    }
}

@Composable
fun FloorFilterApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    FloorFilterAppTheme {
        FloorFilterApp()
    }
}
