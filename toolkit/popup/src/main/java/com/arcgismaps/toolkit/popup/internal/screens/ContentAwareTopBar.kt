/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.arcgismaps.toolkit.popup.PopupState
import com.arcgismaps.toolkit.popup.PopupStateData
import com.arcgismaps.toolkit.popup.R
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationAction
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationRoute

/**
 * A dynamic action bar that adapts its content based on the current navigation state.
 *
 * @param backStackEntry The [NavBackStackEntry] representing the current navigation state.
 * @param state The [PopupState] that holds the current popup state data.
 * @param hasBackStack Indicates if there is a previous route in the navigation stack.
 * @param showCloseIcon Indicates if the close icon should be displayed.
 * @param isNavigationEnabled Indicates if navigation actions are enabled.
 * @param onDismissRequest The callback to invoke when the close button is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun ContentAwareTopBar(
    backStackEntry: NavBackStackEntry,
    state: PopupState,
    hasBackStack: Boolean,
    showCloseIcon: Boolean,
    isNavigationEnabled: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val popupStateData = remember(backStackEntry) { state.getActivePopupStateData() }

    // Callback to handle navigation actions
    val onNavigationAction: (NavigationAction) -> Unit = { action ->
        // execute the action immediately
        when (action) {
            is NavigationAction.NavigateBack -> {
                state.popBackStack(backStackEntry)
            }

            is NavigationAction.Dismiss -> {
                onDismissRequest()
            }

            else -> {}
        }
    }
    val onBackAction: (NavBackStackEntry) -> Unit = { entry ->
        when {
            entry.destination.hasRoute<NavigationRoute.PopupView>() -> {
                // Run the navigation action if the current view is the popup view
                onNavigationAction(NavigationAction.NavigateBack)
            }

            else -> {
                // Pop the back stack if the current view is not the popup view
                state.popBackStack(backStackEntry)
            }
        }
    }
    // Get the title and subtitle for the top bar based on the current navigation state
    val (title, subTitle) = getTopBarTitleAndSubtitle(backStackEntry, popupStateData)
    val navigationEnabled = when {
        // If the current destination is the popup view, only then check if navigation is enabled
        backStackEntry.destination.hasRoute<NavigationRoute.PopupView>() -> {
            isNavigationEnabled
        }
        // For other destinations, always enable back navigation
        else -> true
    }
    Column {
        PopupTitle(
            title = title,
            subTitle = subTitle,
            showCloseIcon = showCloseIcon,
            showBackIcon = hasBackStack,
            isNavigationEnabled = navigationEnabled,
            onBackPressed = {
                onBackAction(backStackEntry)
            },
            onClose = {
                onNavigationAction(NavigationAction.Dismiss)
            },
            modifier = modifier
        )
    }
    // only enable back navigation if there is a previous route
    BackHandler(hasBackStack) {
        onBackAction(backStackEntry)
    }
}

@Composable
private fun getTopBarTitleAndSubtitle(
    backStackEntry: NavBackStackEntry,
    popupStateData: PopupStateData,
): Pair<String, String> {
    var popupTitle by remember(backStackEntry, popupStateData) {
        mutableStateOf(popupStateData.popup.title)
    }

    val defaultTitle = stringResource(R.string.none_selected)
    return when {
        backStackEntry.destination.hasRoute<NavigationRoute.PopupView>() -> {
            Pair(
                popupTitle,
                "" // No subtitle for the main Popup view
            )
        }

        backStackEntry.destination.hasRoute<NavigationRoute.UNFilterView>() -> {
            var title = defaultTitle
            val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
            (popupStateData.stateCollection[route.stateId] as? UtilityAssociationsElementState)?.let { state ->
                state.selectedFilterResult?.filter?.let { filter ->
                    title = filter.title
                }
            }
            Pair(title, popupTitle)
        }

        backStackEntry.destination.hasRoute<NavigationRoute.UNAssociationsView>() -> {
            var title = defaultTitle
            var subTitle = defaultTitle
            val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
            (popupStateData.stateCollection[route.stateId] as? UtilityAssociationsElementState)?.let { state ->
                state.selectedGroupResult?.let { group ->
                    title = group.name
                }
                state.selectedFilterResult?.filter?.let { filter ->
                    subTitle = filter.title
                }
            }
            Pair(title, subTitle)
        }

        backStackEntry.destination.hasRoute<NavigationRoute.UNAssociationDetailView>() -> {
            Pair(stringResource(R.string.association_settings), "")
        }

        else -> {
            Pair(defaultTitle, defaultTitle)
        }
    }
}

/**
 * Represents the title bar of the popup.
 *
 * @param title The title to display.
 * @param subTitle The subtitle to display.
 * @param showCloseIcon Indicates if the close icon should be displayed.
 * @param showBackIcon Indicates if the back icon should be displayed.
 * @param onBackPressed The callback to invoke when the back button is clicked.
 * @param onClose The callback to invoke when the close button is clicked. If null, the close button
 * is not displayed.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
private fun PopupTitle(
    title: String,
    subTitle: String,
    showCloseIcon: Boolean,
    showBackIcon: Boolean,
    isNavigationEnabled: Boolean,
    onBackPressed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (showBackIcon) {
                IconButton(onClick = onBackPressed, enabled = isNavigationEnabled) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (subTitle.isNotEmpty()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (showCloseIcon) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "close popup")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PopupTitlePreview() {
    PopupTitle(
        title = "Structure Boundary",
        subTitle = "Edit feature attributes",
        showCloseIcon = true,
        showBackIcon = false,
        isNavigationEnabled = true,
        onBackPressed = {},
        onClose = {},
        modifier = Modifier.padding(8.dp)
    )
}
