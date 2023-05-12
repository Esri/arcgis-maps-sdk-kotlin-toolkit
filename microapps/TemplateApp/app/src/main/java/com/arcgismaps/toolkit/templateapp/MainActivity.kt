package com.arcgismaps.toolkit.templateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.toolkit.composablemap.ArcGISMap
import com.arcgismaps.toolkit.templateapp.ui.theme.TemplateAppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create(BuildConfig.API_KEY)
        setContent {
            TemplateAppTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val map = remember {
        ArcGISMap(BasemapStyle.ArcGISNavigationNight)
    }
    val viewpoint = Viewpoint(39.8, -98.6, 10e7)
    ArcGISMap(arcGISMap = map,
        viewpoint = viewpoint,
        modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            Card(modifier = Modifier.size(100.dp)) {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TemplateAppTheme {
        App()
    }
}
