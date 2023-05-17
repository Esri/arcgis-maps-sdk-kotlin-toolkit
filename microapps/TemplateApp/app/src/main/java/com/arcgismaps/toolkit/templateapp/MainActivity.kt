package com.arcgismaps.toolkit.templateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.templateapp.screens.MainScreen
import com.arcgismaps.toolkit.templateapp.ui.theme.TemplateAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            TemplateAppTheme {
                TemplateApp()
            }
        }
    }
}

@Composable
fun TemplateApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    TemplateAppTheme {
        TemplateApp()
    }
}
