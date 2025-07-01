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

package com.arcgismaps.toolkit.featureforms.internal.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.FormStateData
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationAction
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A dynamic action bar that adapts its content based on the current navigation state.
 *
 * @param backStackEntry The [NavBackStackEntry] representing the current navigation state.
 * @param state The [FeatureFormState] that holds the current form state data.
 * @param hasBackStack Indicates if there is a previous route in the navigation stack.
 * @param showFormActions Indicates if the form actions (save, discard) should be shown.
 * @param showCloseIcon Indicates if the close icon should be displayed.
 * @param onSaveForm The callback to invoke when the save button is clicked. It takes the current
 * [FeatureForm] and a boolean indicating if the save is followed by a navigation action.
 * @param onDiscardForm The callback to invoke when the discard button is clicked. It takes a boolean
 * indicating if the discard is followed by a navigation action.
 * @param onDismissRequest The callback to invoke when the close button is clicked. If the form has
 * unsaved edits, this in invoked after the save or discard action is completed.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun ContentAwareTopBar(
    backStackEntry: NavBackStackEntry,
    state: FeatureFormState,
    hasBackStack: Boolean,
    showFormActions: Boolean,
    showCloseIcon: Boolean,
    onSaveForm: suspend (FeatureForm, Boolean) -> Result<Unit>,
    onDiscardForm: suspend (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formData = remember(backStackEntry) { state.getActiveFormStateData() }
    val scope = rememberCoroutineScope()
    val hasEdits by formData.featureForm.hasEdits.collectAsState()
    // State to hold the pending navigation action when the form has unsaved edits
    var pendingNavigationAction: NavigationAction by rememberSaveable {
        mutableStateOf(NavigationAction.None)
    }
    // Callback to handle navigation actions based on the form's edit state
    val onNavigationAction: (NavigationAction, Boolean) -> Unit = { action, formHasEdits ->
        if (formHasEdits) {
            // If the form has edits, store the pending action
            pendingNavigationAction = action
        } else {
            // Otherwise, execute the action immediately
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
    }
    val onBackAction : (NavBackStackEntry) -> Unit = { entry ->
        when {
            entry.destination.hasRoute<NavigationRoute.FormView>() -> {
                // Run the navigation action if the current view is the form view
                onNavigationAction(NavigationAction.NavigateBack, hasEdits)
            }

            else -> {
                // Pop the back stack if the current view is not the form view
                state.popBackStack(backStackEntry)
            }
        }
    }
    // Get the title and subtitle for the top bar based on the current navigation state
    val (title, subTitle) = getTopBarTitleAndSubtitle(backStackEntry, formData)
    Column {
        FeatureFormTitle(
            title = title,
            subTitle = subTitle,
            hasEdits = if (showFormActions) hasEdits else false,
            showCloseIcon = showCloseIcon,
            showBackIcon = hasBackStack,
            onBackPressed = {
                onBackAction(backStackEntry)
            },
            onClose = {
                onNavigationAction(NavigationAction.Dismiss, hasEdits)
            },
            onSave = {
                scope.launch {
                    onSaveForm(formData.featureForm, false)
                }
            },
            onDiscard = {
                scope.launch {
                    onDiscardForm(false)
                }
            },
            modifier = modifier
        )
        InitializingExpressions(
            modifier = Modifier.fillMaxWidth(),
            evaluationProvider = { formData.isEvaluatingExpressions.value }
        )
    }
    if (pendingNavigationAction != NavigationAction.None) {
        SaveEditsDialog(
            onDismissRequest = {
                // Clear the pending action when the dialog is dismissed
                pendingNavigationAction = NavigationAction.None
            },
            onSave = {
                scope.launch(Dispatchers.Main) {
                    // Check if the pending action is to navigate back, since NavigateToAssociation
                    // is not triggered by the top bar
                    val willNavigate = pendingNavigationAction == NavigationAction.NavigateBack
                    onSaveForm(formData.featureForm, willNavigate).onSuccess {
                        // Execute the pending navigation action after saving
                        onNavigationAction(pendingNavigationAction, false)
                    }
                    pendingNavigationAction = NavigationAction.None
                }
            },
            onDiscard = {
                scope.launch(Dispatchers.Main) {
                    // Check if the pending action is to navigate back, since NavigateToAssociation
                    // is not triggered by the top bar
                    val willNavigate = pendingNavigationAction == NavigationAction.NavigateBack
                    onDiscardForm(willNavigate)
                    onNavigationAction(pendingNavigationAction, false)
                    pendingNavigationAction = NavigationAction.None
                }
            }
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
    formData: FormStateData,
): Pair<String, String> {
    var formTitle by remember(backStackEntry, formData) {
        mutableStateOf(formData.featureForm.title.value)
    }

    LaunchedEffect(backStackEntry, formData) {
        formData.featureForm.title.collectLatest {
            formTitle = it
        }
    }

    val defaultTitle = stringResource(R.string.none_selected)
    return when {
        backStackEntry.destination.hasRoute<NavigationRoute.FormView>() -> {
            Pair(
                formTitle,
                formData.featureForm.description
            )
        }

        backStackEntry.destination.hasRoute<NavigationRoute.UNFilterView>() -> {
            var title = defaultTitle
            val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
            (formData.stateCollection[route.stateId] as? UtilityAssociationsElementState)?.let { state ->
                state.selectedFilterResult?.filter?.let { filter ->
                    title = filter.title
                }
            }
            Pair(title, formTitle)
        }

        backStackEntry.destination.hasRoute<NavigationRoute.UNAssociationsView>() -> {
            var title = defaultTitle
            var subTitle = defaultTitle
            val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
            (formData.stateCollection[route.stateId] as? UtilityAssociationsElementState)?.let { state ->
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
 * Represents the title bar of the form.
 *
 * @param title The title to display.
 * @param subTitle The subtitle to display.
 * @param hasEdits Indicates if the form has unsaved edits. An unsaved edits indicator is displayed
 * along with the save and discard buttons if this is true.
 * @param showCloseIcon Indicates if the close icon should be displayed.
 * @param showBackIcon Indicates if the back icon should be displayed.
 * @param onBackPressed The callback to invoke when the back button is clicked.
 * @param onClose The callback to invoke when the close button is clicked. If null, the close button
 * is not displayed.
 * @param onSave The callback to invoke when the save button is clicked.
 * @param onDiscard The callback to invoke when the discard button is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
private fun FeatureFormTitle(
    title: String,
    subTitle: String,
    hasEdits: Boolean,
    showCloseIcon: Boolean,
    showBackIcon: Boolean,
    onBackPressed: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (showBackIcon) {
                IconButton(onClick = onBackPressed) {
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
                    if (hasEdits) {
                        Spacer(Modifier.width(8.dp))
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = Color(0xFFB3261E))
                        }
                    }
                }
                if (subTitle.isNotEmpty()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (hasEdits) {
                    Text(
                        text = "Unsaved Changes",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (showCloseIcon) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "close form")
                }
            }
        }
        AnimatedVisibility(visible = hasEdits) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSave) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onDiscard) {
                    Text(
                        text = stringResource(R.string.discard),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 1f else 0f,
        label = "evaluation loading alpha"
    )
    Surface(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        LinearProgressIndicator(modifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormTitlePreview() {
    FeatureFormTitle(
        title = "Structure Boundary",
        subTitle = "Edit feature attributes",
        hasEdits = true,
        showCloseIcon = true,
        showBackIcon = false,
        onBackPressed = {},
        onClose = {},
        onSave = {},
        onDiscard = {},
        modifier = Modifier.padding(8.dp)
    )
}
