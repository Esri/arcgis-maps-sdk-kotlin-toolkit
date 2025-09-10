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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupAttachment
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.popup.UtilityAssociationsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.state.mutablePopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextElementState
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.PopupNavHost
import com.arcgismaps.toolkit.popup.internal.screens.ContentAwareTopBar
import kotlinx.coroutines.CoroutineScope

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
@Composable
public fun Popup(popup: Popup, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val stateData = remember(popup) {
        PopupState(popup, scope)
    }

    if (stateData.getActivePopupStateData().initialEvaluation.value) {
        Popup(popup, stateData, modifier, showCloseIcon = false)
    }
}

@Composable
public fun Popup(
    popup: Popup,
    popupState: PopupState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showCloseIcon: Boolean = true,
    isNavigationEnabled : Boolean = true,
) {
    // Add the provided state collection to the store.
    val popupStateData = PopupStateData(popup)
//    popupState.store.addLast(popupStateData)
//    LaunchedEffect(popup) {
//        popupState.evaluateExpressions()
//    }
//    val states = rememberStates(popup, popupState.attachments, rememberCoroutineScope())
//    popupState.setStates(states)

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

/**
 * Creates and remembers state objects for all the supported element types that are part of the
 * provided Popup. These state objects are returned as part of a [PopupElementStateCollection].
 *
 * @param popup the [Popup] to create the states for.
 * @return returns the [PopupElementStateCollection] created.
 */
//@Composable
internal fun rememberStates(
    popup: Popup,
    attachments: List<PopupAttachment>,
    coroutineScope: CoroutineScope
): PopupElementStateCollection {
    val states = mutablePopupElementStateCollection()
    popup.evaluatedElements.forEach { element ->
        when (element) {
            is TextPopupElement -> {
                states.add(
                    element,
                    TextElementState(element = element, popup = popup)
                )
            }

//            is AttachmentsPopupElement -> {
//                states.add(
//                    element,
//                    rememberAttachmentsElementState(
//                        popup = popup,
//                        element = element,
//                        attachments = attachments
//                    )
//                )
//            }

            is FieldsPopupElement -> {
                states.add(
                    element,
                    FieldsElementState(element = element, popup = popup)
                )
            }

//            is MediaPopupElement -> {
//                states.add(
//                    element,
//                    rememberMediaElementState(element = element, popup = popup)
//                )
//            }

            is UtilityAssociationsPopupElement -> {
                states.add(
                    element,
                    UtilityAssociationsElementState(element, coroutineScope)
                )
            }

            else -> {
                // TODO remove for release
                println("encountered element of type ${element::class.java}")
            }
        }
    }

    return states
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
