package com.arcgismaps.toolkit.featureformsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.featureformsapp.screens.MapScreen
import com.arcgismaps.toolkit.featureformsapp.ui.theme.FeatureFormsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            FeatureFormsAppTheme {
                FeatureFormsApp()
            }
        }
    }
}

@Composable
fun FeatureFormsApp() {
    MapScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    FeatureFormsAppTheme {
        FeatureFormsApp()
    }
}
