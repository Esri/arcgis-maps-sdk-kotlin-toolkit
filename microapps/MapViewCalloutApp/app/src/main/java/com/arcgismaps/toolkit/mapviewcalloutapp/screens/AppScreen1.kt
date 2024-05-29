package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.mapviewcalloutapp.MapViewModel
import kotlin.math.roundToInt
@Composable
fun AppScreen1() {

    val viewModel = remember { MapViewModel() }
    val mapPoint = viewModel.mapPoint.collectAsState().value

    var calloutVisibility by remember { mutableStateOf(false) }
    var rotateOffsetWithGeoView by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column {
        CalloutOptionsBox(
            calloutVisibility = calloutVisibility,
            isCalloutRotationEnabled = rotateOffsetWithGeoView,
            offset = offset,
            onVisibilityToggled = { calloutVisibility = !calloutVisibility },
            onCalloutOffsetRotationToggled = { rotateOffsetWithGeoView = !rotateOffsetWithGeoView },
            onXAxisOffsetChanged = {
                offset = Offset(it,offset.y)
            },
            onYAxisOffsetChanged = {
                offset = Offset(offset.x,it)
            }
        )

        MapView(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            arcGISMap = viewModel.arcGISMap,
            graphicsOverlays = remember { listOf(viewModel.tapLocationGraphicsOverlay) },
            onSingleTapConfirmed = viewModel::setMapPoint,
            content = if (mapPoint != null && calloutVisibility) {
                {
                    Callout(
                        modifier = Modifier.size(100.dp),
                        location = mapPoint,
                        rotateOffsetWithGeoView = rotateOffsetWithGeoView
                    )
                    {
                        Text("Tapped location: ${mapPoint.x.roundToInt()},${mapPoint.y.roundToInt()}")
                    }

                }
            } else {
                null
            }
        )
    }
}

@Composable
fun CalloutOptionsBox(
    calloutVisibility: Boolean,
    onVisibilityToggled: () -> Unit,
    isCalloutRotationEnabled: Boolean,
    onCalloutOffsetRotationToggled: () -> Unit,
    onXAxisOffsetChanged: (Float) -> Unit,
    onYAxisOffsetChanged: (Float) -> Unit,
    offset: Offset,

    ) {
    Column(Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Show Callout")
            Checkbox(
                checked = calloutVisibility,
                onCheckedChange = { onVisibilityToggled() }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Rotate offset")
            Checkbox(
                checked = isCalloutRotationEnabled,
                onCheckedChange = { onCalloutOffsetRotationToggled() }
            )
        }

        Text(text = "Offset")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "X-Axis")
            TextField(
                value = offset.x.toString(),
                onValueChange = { value ->
                    onXAxisOffsetChanged(value.toFloat())
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Enter an integer") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Y-Axis")
            TextField(
                value = offset.y.toString(),
                onValueChange = { value ->
                    onYAxisOffsetChanged(value.toFloat())
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Enter an integer") }
            )
        }
    }
}