/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.mapviewcalloutapp.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.Callout
import com.arcgismaps.toolkit.geoviewcompose.MapView

@Composable
fun MainScreen(viewModel: MapViewModel) {

    var mapPoint by remember {
        mutableStateOf<Point?>(null)
    }
    Log.e("TAG", "MainScreen: recompose $mapPoint")
    val visible by remember {
        derivedStateOf {
            mapPoint != null
        }
    }
    val density = LocalDensity.current

    val state = remember {
        MutableTransitionState(false).apply {
            // Start the animation immediately.
            targetState = true
        }
    }

//    Column {
//
//        Button(
//            onClick = { visible = !visible }
////        modifier = Modifier.fillMaxSize()
//        ) {
//            Text("Set Map Point")
//        }
////        if (visible) {
//            AnimatedVisibility(
//                visible = visible, enter = scaleIn(),
//                exit = scaleOut(),
//            ) {
////                Text("Hello", Modifier.fillMaxWidth().height(200.dp))
//                Button(
//                    onClick = {},
//                   modifier = Modifier
//                       .animateContentSize ()
////                       .fillMaxSize()
//                ) {
//                    Text("Set Map Point")
//                }
//            }
//        }

    Column {
        Button(
            onClick = { mapPoint = null }
//        modifier = Modifier.fillMaxSize()
        ) {
            Text("Set Box Visibility")
        }

        MapView(
            modifier = Modifier.fillMaxSize(),
            arcGISMap = viewModel.arcGISMap,
            onSingleTapConfirmed = {
                mapPoint = it.mapPoint
            },
            onLongPress = { },
            onDoubleTap = { },
            content = {
                val movableContent = movableContentOf {
                    if (mapPoint != null) {
                        Callout(location = mapPoint!!) {
                            Text(
                                "Hello, World 0!",
                                color = Color.Green
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    label = "",
                ) {
                    movableContent()
                }
            }
        )
    }
}

//@Composable
//fun MainScreen(viewModel: MapViewModel) {
//
//    val mapPoint = viewModel.mapPoint.collectAsState().value
//    var isDismissed by remember { mutableStateOf(false) }
//    val density = LocalDensity.current
//
//    SubComposeLayoutDemo()
//
//}

@Composable
fun SubComposeLayoutDemo() {
    var isVisible by remember { mutableStateOf(false) }
    Column {

        Button(
            onClick = { isVisible = !isVisible }
//        modifier = Modifier.fillMaxSize()
        ) {
            Text("Set Box Visibility")
        }
//        if (visible) {
        AnimatedVisibility(
            visible = isVisible, enter = scaleIn(),
            exit = scaleOut(),
        ) {

            ResizeWidthColumn(Modifier.fillMaxWidth(), true) {

                Box(
                    modifier = Modifier
                        .background(Color.Red)
                ) {
                    Text("Hello")
                }

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(Color.Red)
                ) {
                    Text("This is a long messsage \n and its longer")
                }
            }
        }
    }
}

@Composable
fun ResizeWidthColumn(modifier: Modifier, resize: Boolean, mainContent: @Composable () -> Unit) {
    SubcomposeLayout(modifier) { constraints ->
        val mainPlaceables = subcompose(SlotsEnum.Main, mainContent).map {
            // Here we measure the width/height of the child Composables
            it.measure(Constraints())
        }

        //Here we find the max width/height of the child Composables
        val maxSize = mainPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
            IntSize(
                width = maxOf(currentMax.width, placeable.width),
                height = maxOf(currentMax.height, placeable.height)
            )
        }

        val resizedPlaceables: List<Placeable> =
            subcompose(SlotsEnum.Dependent, mainContent).map {
                if (resize) {
                    /** Here we rewrite the child Composables to have the width of
                     * widest Composable
                     */
                    it.measure(
                        Constraints(
                            minWidth = maxSize.width
                        )
                    )
                } else {
                    // Ask the child for its preferred size.
                    it.measure(Constraints())
                }
            }

        /**
         * We can place the Composables on the screen
         * with layout() and the place() functions
         */

        layout(constraints.maxWidth, constraints.maxHeight) {
            resizedPlaceables.forEachIndexed { index, placeable ->
                val widthStart = resizedPlaceables.take(index).sumOf { it.measuredHeight }
                placeable.place(0, widthStart)
            }
        }
    }
}


enum class SlotsEnum {
    Main,
    Dependent

}
