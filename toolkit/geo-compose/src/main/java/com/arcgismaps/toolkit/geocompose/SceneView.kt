/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.view.DoubleTapEvent
import com.arcgismaps.mapping.view.DownEvent
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.RotationChangeEvent
import com.arcgismaps.mapping.view.ScaleChangeEvent
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.TwoPointerTapEvent
import com.arcgismaps.mapping.view.UpEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A compose equivalent of the view-based [com.arcgismaps.mapping.view.SceneView].
 *
 * @param modifier Modifier to be applied to the composable SceneView
 * @param arcGISScene the [ArcGISScene] to be rendered by this composable SceneView
 *  * @param onInteractingChanged lambda invoked when the user starts and ends interacting with the composable SceneView
 *  * @param onRotate lambda invoked when a user performs a rotation gesture on the composable SceneView
 *  * @param onScale lambda invoked when a user performs a pinch gesture on the composable SceneView
 *  * @param onUp lambda invoked when the user removes all their pointers from the composable SceneView
 *  * @param onDown lambda invoked when the user first presses on the composable SceneView
 *  * @param onSingleTapConfirmed lambda invoked when the user taps once on the composable SceneView
 *  * @param onDoubleTap lambda invoked the user double taps on the composable SceneView
 *  * @param onLongPress lambda invoked when a user holds a pointer on the composable SceneView
 *  * @param onTwoPointerTap lambda invoked when a user taps two pointers on the composable SceneView
 *  * @param onPan lambda invoked when a user drags a pointer or pointers across composable SceneView
 * @since 200.4.0
 */
@Composable
public fun SceneView(
    modifier: Modifier = Modifier,
    arcGISScene: ArcGISScene? = null,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)? = null,
    onRotate: ((RotationChangeEvent) -> Unit)? = null,
    onScale: ((ScaleChangeEvent) -> Unit)? = null,
    onUp: ((UpEvent) -> Unit)? = null,
    onDown: ((DownEvent) -> Unit)? = null,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)? = null,
    onDoubleTap: ((DoubleTapEvent) -> Unit)? = null,
    onLongPress: ((LongPressEvent) -> Unit)? = null,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)? = null,
    onPan: ((PanChangeEvent) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val sceneView = remember { com.arcgismaps.mapping.view.SceneView(context) }

    AndroidView(
        modifier = modifier.semantics { contentDescription = "SceneView" },
        factory = { sceneView },
        update = {
            it.scene = arcGISScene
        })

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(sceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(sceneView)
            sceneView.onDestroy(lifecycleOwner)
        }
    }

    SceneViewEventHandler(
        sceneView,
        onInteractingChanged,
        onRotate,
        onScale,
        onUp,
        onDown,
        onSingleTapConfirmed,
        onDoubleTap,
        onLongPress,
        onTwoPointerTap,
        onPan,
    )
}

/**
 * Sets up the callbacks for all the view-based [sceneView] events.
 */
@Composable
private fun SceneViewEventHandler(
    sceneView: SceneView,
    onInteractingChanged: ((isInteracting: Boolean) -> Unit)?,
    onRotate: ((RotationChangeEvent) -> Unit)?,
    onScale: ((ScaleChangeEvent) -> Unit)?,
    onUp: ((UpEvent) -> Unit)?,
    onDown: ((DownEvent) -> Unit)?,
    onSingleTapConfirmed: ((SingleTapConfirmedEvent) -> Unit)?,
    onDoubleTap: ((DoubleTapEvent) -> Unit)?,
    onLongPress: ((LongPressEvent) -> Unit)?,
    onTwoPointerTap: ((TwoPointerTapEvent) -> Unit)?,
    onPan: ((PanChangeEvent) -> Unit)?,
) {
    val currentOnInteractingChanged by rememberUpdatedState(onInteractingChanged)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnUp by rememberUpdatedState(onUp)
    val currentOnDown by rememberUpdatedState(onDown)
    val currentSingleTapConfirmed by rememberUpdatedState(onSingleTapConfirmed)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTwoPointerTap by rememberUpdatedState(onTwoPointerTap)
    val currentOnPan by rememberUpdatedState(onPan)

    LaunchedEffect(Unit) {
        launch(Dispatchers.Main.immediate) {
            sceneView.isInteracting.collect { isInteracting ->
                currentOnInteractingChanged?.invoke(isInteracting)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onRotate.collect { rotationChangeEvent ->
                currentOnRotate?.invoke(rotationChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onScale.collect { scaleChangeEvent ->
                currentOnScale?.invoke(scaleChangeEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onUp.collect { upEvent ->
                currentOnUp?.invoke(upEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onDown.collect { downEvent ->
                currentOnDown?.invoke(downEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onSingleTapConfirmed.collect { singleTapConfirmedEvent ->
                currentSingleTapConfirmed?.invoke(singleTapConfirmedEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onDoubleTap.collect { doubleTapEvent ->
                currentOnDoubleTap?.invoke(doubleTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onLongPress.collect { longPressEvent ->
                currentOnLongPress?.invoke(longPressEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onTwoPointerTap.collect { twoPointerTapEvent ->
                currentOnTwoPointerTap?.invoke(twoPointerTapEvent)
            }
        }
        launch(Dispatchers.Main.immediate) {
            sceneView.onPan.collect { panChangeEvent ->
                currentOnPan?.invoke(panChangeEvent)
            }
        }
    }
}
