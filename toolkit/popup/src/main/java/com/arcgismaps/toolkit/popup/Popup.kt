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

package com.arcgismaps.toolkit.popup

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.mapping.popup.UtilityAssociationsPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.popup.internal.navigation.PopupNavHost
import com.arcgismaps.toolkit.popup.internal.screens.ContentAwareTopBar

/**
 * A composable Popup toolkit component that enables users to see Popup content in a
 * layer that have been configured externally.
 *
 * Popups may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)).
 *
 * Note : Even though the [Popup] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param popup The [Popup] configuration.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * Popup.
 *
 * @since 200.5.0
 */
@Deprecated(
    message = "Use the overload that uses the PopupState object. This will become an error" +
            " in a future release.",
    level = DeprecationLevel.WARNING
)
@Composable
public fun Popup(popup: Popup, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val stateData = remember(popup) {
        PopupState(
            popup,
            scope,
            // Ignore the UtilityAssociationsFormElement as it is not supported with this API
            ignoreList = setOf(
                UtilityAssociationsPopupElement::class.java
            )
        )
    }
    Popup(stateData, modifier, showCloseIcon = false)
}

/**
 * A composable Popup toolkit component that enables users to see Popup content in a
 * layer that have been configured externally.
 *
 * Popups may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)).
 *
 * Note : Even though the [Popup] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param popupState The [PopupState] object that holds the state of the Popup.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * Popup.
 * @param onDismiss Callback that is invoked when the user clicks the close icon in the top app bar.
 * @param showCloseIcon Flag to indicate if the close icon should be shown in the top app bar. If true, the [onDismiss]
 * callback will be invoked when the close icon is clicked. Default is true.
 * @param isNavigationEnabled Indicates if the navigation is enabled for the popup when there are
 * [UtilityAssociationsPopupElement]s present. When true, the user can navigate to associated features
 * and back. If false, this navigation is disabled. Default is true
 *
 * @since 300.0.0
 */
@Composable
public fun Popup(
    popupState: PopupState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
    isNavigationEnabled : Boolean = true
) {
    val popup = popupState.popup
    val dynamicEntity = (popup.geoElement as? DynamicEntity)
    // If the popup is for a dynamic entity, we want to refresh the popup periodically
    // to get the latest data.
    var lastUpdatedEntityId by rememberSaveable(dynamicEntity) { mutableLongStateOf(dynamicEntity?.id ?: -1) }
    if (dynamicEntity != null) {
        LaunchedEffect(popup) {
            dynamicEntity.dynamicEntityChangedEvent.collect {
                // briefly show the initializing screen so it is clear the entity just pulsed
                // and values may have changed.
                popupState.popup.evaluateExpressions()
                lastUpdatedEntityId = it.receivedObservation?.id ?: -1
            }
        }
    }

    val navController = rememberNavController(popupState)
    popupState.setNavigationCallback { route ->
        navController.navigate(route)
    }
    popupState.setNavigateBack {
        navController.navigateUp()
    }

    PopupLayout(
        topBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            // Track if there is a back stack entry
            val hasBackStack = remember(backStackEntry) {
                navController.previousBackStackEntry != null
            }
            backStackEntry?.let { entry ->
                ContentAwareTopBar(
                    backStackEntry = entry,
                    state = popupState,
                    onDismissRequest = onDismiss,
                    hasBackStack = hasBackStack,
                    showCloseIcon = showCloseIcon,
                    isNavigationEnabled = isNavigationEnabled,
                    modifier = Modifier
                        .padding(
                            vertical = 8.dp,
                            horizontal = if (hasBackStack) 8.dp else 16.dp
                        )
                        .fillMaxWidth(),
                )
            }
        },
        content = {
            PopupNavHost(
                navController = navController,
                state = popupState,
                refreshed = lastUpdatedEntityId,
                isNavigationEnabled = isNavigationEnabled,
                modifier = Modifier.fillMaxSize()
            )
        },
        modifier = modifier
    )
    DisposableEffect(popupState) {
        onDispose {
            // Clear the navigation actions when the composition is disposed
            popupState.setNavigationCallback(null)
            popupState.setNavigateBack(null)
        }
    }

}

@Composable
internal fun PopupLayout(
    topBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        topBar()
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        content()
    }
}

@Composable
internal fun rememberNavController(vararg inputs: Any): NavHostController {
    val context = LocalContext.current
    rememberNavController()
    return rememberSaveable(
        inputs = inputs, saver = Saver(
            save = { it.saveState() },
            restore = { createNavController(context).apply { restoreState(it) } }
        )) {
        createNavController(context)
    }
}

private fun createNavController(context: Context): NavHostController {
    return NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }
}
