package com.arcgismaps.toolkit.featureforms.components.datetime.picker.time

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * It depends on the state of accessibility services to determine the current state of touch
 * exploration services.
 */
@Composable
internal fun touchExplorationState(): State<Boolean> {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    val listener = remember { Listener() }

    LocalLifecycleOwner.current.lifecycle.ObserveState(
        handleEvent = { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listener.register(accessibilityManager)
            }
        },
        onDispose = {
            listener.unregister(accessibilityManager)
        }
    )

    return remember { derivedStateOf { listener.isEnabled() } }
}

@Composable
private fun Lifecycle.ObserveState(
    handleEvent: (Lifecycle.Event) -> Unit = {},
    onDispose: () -> Unit = {}
) {
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            handleEvent(event)
        }
        this@ObserveState.addObserver(observer)
        onDispose {
            onDispose()
            this@ObserveState.removeObserver(observer)
        }
    }
}

private class Listener : AccessibilityStateChangeListener, TouchExplorationStateChangeListener {
    private var accessibilityEnabled by mutableStateOf(false)
    private var touchExplorationEnabled by mutableStateOf(false)

    fun isEnabled() = accessibilityEnabled && touchExplorationEnabled

    override fun onAccessibilityStateChanged(it: Boolean) {
        accessibilityEnabled = it
    }

    override fun onTouchExplorationStateChanged(it: Boolean) {
        touchExplorationEnabled = it
    }

    fun register(am: AccessibilityManager) {
        accessibilityEnabled = am.isEnabled
        touchExplorationEnabled = am.isTouchExplorationEnabled

        am.addTouchExplorationStateChangeListener(this)
        am.addAccessibilityStateChangeListener(this)
    }

    fun unregister(am: AccessibilityManager) {
        am.removeTouchExplorationStateChangeListener(this)
        am.removeAccessibilityStateChangeListener(this)
    }
}
