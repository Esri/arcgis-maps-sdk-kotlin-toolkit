package com.arcgismaps.toolkit.exampleapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.exampleapp.ui.theme.ExampleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.apiKey =
            ApiKey.create("")
        setContent {
            ExampleAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val map = remember {
        ArcGISMap(BasemapStyle.ArcGISNavigationNight)
    }
    val viewpointAmerica = Viewpoint(39.8, -98.6, 10e7)

    ArcGISMapScaffoldView(
        modifier = Modifier.fillMaxSize(),
        arcGISMap = map,
        viewpoint = viewpointAmerica,
        topLeftActions = {
            ActionItem(text = "Top L")
        },
        topRightActions = {
            ActionItem(text = "Top R")
        },
        bottomLeftActions = {
            ActionItem(text = "Bottom L")
            ActionItem(text = "Bottom L2")
        },
        bottomRightActions = {
            ActionItem(text = "Bottom R")
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionItem(text: String) {
    Card(
        modifier = Modifier
            .height(100.dp)
            .width(100.dp),
        onClick = {
            Log.d(MainActivity::getLocalClassName.toString(), "ActionItem: $text")
        }
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ExampleAppTheme {
        App()
    }
}
