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

package com.arcgismaps.toolkit.featureforms

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormInput
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.rememberAttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.rememberBarcodeTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.MutableFormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.internal.components.base.rememberBaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberRadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.rememberSwitchFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.rememberDateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.rememberFormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.rememberTextFormElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.Associations
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogRequester
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormColorScheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormDefaults
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * The "property" determines the behavior of when the validation errors are visible.
 */
@Deprecated(
    message = "Deprecated without replacement",
    level = DeprecationLevel.WARNING
)
@Immutable
public sealed class ValidationErrorVisibility {

    /**
     * Indicates that the validation errors are only visible for editable fields that have
     * received focus.
     */
    public object Automatic : ValidationErrorVisibility()

    /**
     * Indicates the validation is run for all the editable fields regardless of their focus state,
     * and any errors are shown.
     */
    public object Visible : ValidationErrorVisibility()
}

@Serializable
internal sealed class NavigationRoute {

    @Serializable
    data object FormView : NavigationRoute()

    @Serializable
    data class UNFilterView(
        val stateId: Int,
        val selectedFilterIndex: Int
    ) : NavigationRoute()

    @Serializable
    data class UNAssociationsView(
        val stateId: Int,
        val selectedFilterIndex: Int,
        val selectedGroupIndex: Int
    ) : NavigationRoute()
}

@Deprecated(
    message = "Maintained for binary compatibility. Use the overload that accepts a colorScheme and typography.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
) {
    FeatureForm(
        featureForm = featureForm,
        modifier = modifier,
        validationErrorVisibility = validationErrorVisibility,
        colorScheme = FeatureFormDefaults.colorScheme(),
        typography = FeatureFormDefaults.typography(),
        onBarcodeButtonClick = null
    )
}

@Deprecated(
    message = "Maintained for binary compatibility. Use the overload that provides the barcode accessory tap callback.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    FeatureForm(
        featureForm = featureForm,
        modifier = modifier,
        validationErrorVisibility = validationErrorVisibility,
        colorScheme = colorScheme,
        typography = typography,
        onBarcodeButtonClick = null
    )
}

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using a [FeatureForm] have been configured externally.
 *
 * The [FeatureForm] component supports the following [FormElement] types as part of its configuration.
 * - [AttachmentsFormElement]
 * - [FieldFormElement] with the following [FormInput] types -
 *     * [BarcodeScannerFormInput]
 *     * [ComboBoxFormInput]
 *     * [DateTimePickerFormInput]
 *     * [RadioButtonsFormInput]
 *     * [SwitchFormInput]
 *     * [TextAreaFormInput]
 *     * [TextBoxFormInput]
 * - [GroupFormElement]
 * - [TextFormElement]
 *
 * @param featureForm the [FeatureForm] object to use
 * @param modifier the modifier to apply to this layout.
 * @param validationErrorVisibility This property is deprecated and has no effect.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.4.0
 */
@Deprecated(
    message = "Use the overload that uses the FeatureFormState object. This will become an error" +
        " in a future release.",
    level = DeprecationLevel.WARNING
)
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val scope = rememberCoroutineScope()
    val states = rememberStates(
        form = featureForm,
        elements = featureForm.elements,
        scope = scope
    )
    val state = remember(featureForm) {
        FeatureFormState(
            featureForm = featureForm,
            stateCollection = states,
            coroutineScope = scope
        )
    }
    FeatureForm(
        state = state,
        modifier = modifier,
        colorScheme = colorScheme,
        typography = typography,
        onBarcodeButtonClick = onBarcodeButtonClick,
        onDismiss = {}
    )
}

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using a [FeatureForm] have been configured externally. Forms may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)) and can be obtained from either
 * an `ArcGISFeature`, `ArcGISFeatureTable`, `FeatureLayer` or `SubtypeSublayer`.
 *
 * The [FeatureForm] component supports the following [FormElement] types as part of its configuration.
 * - [AttachmentsFormElement]
 * - [FieldFormElement] with the following [FormInput] types -
 *     * [BarcodeScannerFormInput]
 *     * [ComboBoxFormInput]
 *     * [DateTimePickerFormInput]
 *     * [RadioButtonsFormInput]
 *     * [SwitchFormInput]
 *     * [TextAreaFormInput]
 *     * [TextBoxFormInput]
 * - [GroupFormElement]
 * - [TextFormElement]
 * - [UtilityAssociationsFormElement]
 *
 * For any elements of input type [BarcodeScannerFormInput], a default barcode scanner based on MLKit
 * is provided. The scanner requires the [Manifest.permission.CAMERA] permission to be granted.
 * A callback is also provided via the [onBarcodeButtonClick] parameter, which is invoked with
 * the [FieldFormElement] when its barcode accessory is clicked. This can be used to provide a custom
 * barcode scanning experience. Simply call [FieldFormElement.updateValue] with the scanned barcode
 * value to update the field value.
 *
 * For adding any attachments, camera permissions are required. If the permissions are not granted,
 * then the specific functionality is disabled in the form.
 *
 * Any [AttachmentsFormElement] present in the [FeatureForm.elements] collection are not
 * currently supported. A default attachments editing support is provided using the
 * [FeatureForm.defaultAttachmentsElement] property.
 *
 * The colors and typography for the Form can use customized using [FeatureFormColorScheme] and
 * [FeatureFormTypography]. This customization is built on top of [MaterialTheme].
 * If a custom color is specified in both the color scheme and the typography, the color from the
 * color scheme will take precedence and will be merged with the text style, if one is provided.
 *
 * @param state the [FeatureFormState] object that contains the state of the form.
 * @param modifier the modifier to apply to this layout.
 * @param onBarcodeButtonClick A callback that is invoked when the barcode accessory is clicked.
 * The callback is invoked with the [FieldFormElement] that has the barcode accessory. If null, the
 * default barcode scanner is used.
 * @param colorScheme The [FeatureFormColorScheme] to use for the FeatureForm.
 * @param typography The [FeatureFormTypography] to use for the FeatureForm.
 *
 * @since 200.7.0
 */
@Composable
public fun FeatureForm(
    state: FeatureFormState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)?,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)? = null,
    colorScheme: FeatureFormColorScheme = FeatureFormDefaults.colorScheme(),
    typography: FeatureFormTypography = FeatureFormDefaults.typography(),
) {
    val navController = rememberNavController(state)
    val scope = rememberCoroutineScope()
    state.setNavController(navController)
    val dialogRequester = LocalDialogRequester.current
    val onDismissAction = remember(state.activeFeatureForm, onDismiss, dialogRequester) {
        onDismiss?.let {
            createSaveEditsDialogAction(
                featureForm = state.activeFeatureForm,
                navigationAction = onDismiss,
                dialogRequester = dialogRequester
            )
        }
    }
    val onSaveAction: suspend (FeatureForm) -> Unit = { form ->
        val errorCount = form.validationErrors.value.entries.count()
        if (errorCount == 0) {
            form.finishEditing()
        } else {
            showValidationErrorsDialog(
                onDismiss = {},
                count = errorCount,
                dialogRequester
            )
        }
    }
    val onDiscardAction: (FeatureForm) -> Unit = { form ->
        form.discardEdits()
    }
    FeatureFormTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        NavHost(
            navController,
            startDestination = NavigationRoute.FormView,
            modifier = modifier,
            enterTransition = { slideInHorizontally { h -> h } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { h -> h } }
        ) {
            composable<NavigationRoute.FormView> { backStackEntry ->
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val featureForm = stateData.featureForm
                val states = stateData.stateCollection
                val hasBackStack = remember(featureForm) { state.hasBackStack() }
                val onBackAction = createSaveEditsDialogAction(
                    featureForm = featureForm,
                    navigationAction = state::popBackStack,
                    dialogRequester = dialogRequester
                )
                val hasEdits by featureForm.hasEdits.collectAsState()
                FeatureFormLayout(
                    title = {
                        val title by featureForm.title.collectAsState()
                        FeatureFormTitle(
                            title = title,
                            subTitle = featureForm.description,
                            hasEdits = hasEdits,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = if (hasBackStack) 8.dp else 16.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = if (hasBackStack) onBackAction else null,
                            onClose = onDismissAction,
                            onSave = {
                                scope.launch { onSaveAction(featureForm) }
                            },
                            onDiscard = { onDiscardAction(featureForm) }
                        )
                        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
                            state.evaluatingExpressions
                        }
                    },
                    content = {
                        FormContent(
                            formStateData = stateData,
                            onBarcodeButtonClick = onBarcodeButtonClick,
                            onUtilityAssociationFilterClick = { stateId, index ->
                                val route = NavigationRoute.UNFilterView(
                                    stateId = stateId,
                                    selectedFilterIndex = index
                                )
                                navController.navigate(route)
                            }
                        )
                    }
                )
                // only enable back navigation if there is a previous route
                BackHandler(hasBackStack) {
                    onBackAction()
                }
                FeatureFormDialog(states)
            }

            composable<NavigationRoute.UNFilterView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val unState =
                    stateData.stateCollection[routeData.stateId] as? UtilityAssociationsElementState
                        ?: return@composable
                val route = backStackEntry.toRoute<NavigationRoute.UNFilterView>()
                val filters by unState.filters
                val filter = remember(filters) { filters.getOrNull(route.selectedFilterIndex) }
                if (filter == null) return@composable
                val hasEdits by stateData.featureForm.hasEdits.collectAsState()
                FeatureFormLayout(
                    title = {
                        val title by stateData.featureForm.title.collectAsState()
                        FeatureFormTitle(
                            title = filter.filter.title,
                            subTitle = title,
                            hasEdits = hasEdits,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 8.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = navController::popBackStack,
                            onClose = onDismissAction,
                            onSave = {
                                scope.launch { onSaveAction(stateData.featureForm) }
                            },
                            onDiscard = { onDiscardAction(stateData.featureForm) }
                        )
                    },
                    content = {
                        UtilityAssociationFilter(
                            filter = filter,
                            onGroupClick = { index ->
                                val newRoute = NavigationRoute.UNAssociationsView(
                                    stateId = unState.id,
                                    selectedFilterIndex = route.selectedFilterIndex,
                                    selectedGroupIndex = index
                                )
                                navController.navigate(newRoute)
                            },
                            onBackPressed = navController::popBackStack,
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                )
            }

            composable<NavigationRoute.UNAssociationsView> { backStackEntry ->
                val routeData = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val stateData = remember(backStackEntry) { state.getActiveStateData() }
                val unState =
                    stateData.stateCollection[routeData.stateId] as? UtilityAssociationsElementState
                        ?: return@composable
                val route = backStackEntry.toRoute<NavigationRoute.UNAssociationsView>()
                val filters by unState.filters
                val filter = remember(filters) { filters.getOrNull(route.selectedFilterIndex) }
                if (filter == null) return@composable
                val group =
                    remember(filters) { filter.groups.getOrNull(route.selectedGroupIndex) }
                if (group == null) return@composable
                val hasEdits by stateData.featureForm.hasEdits.collectAsState()
                FeatureFormLayout(
                    title = {
                        FeatureFormTitle(
                            title = group.name,
                            subTitle = filter.filter.title,
                            hasEdits = hasEdits,
                            modifier = Modifier
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 8.dp
                                )
                                .fillMaxWidth(),
                            onBackPressed = navController::popBackStack,
                            onClose = onDismissAction,
                            onSave = {
                                scope.launch { onSaveAction(stateData.featureForm) }
                            },
                            onDiscard = { onDiscardAction(stateData.featureForm) }
                        )
                    },
                    content = {
                        Associations(
                            state = group,
                            onItemClick = { info ->
                                if (stateData.featureForm.hasEdits.value) {
                                    val dialog = createSaveEditsDialog(
                                        featureForm = stateData.featureForm,
                                        navigationAction = {
                                            state.navigateTo(FeatureForm(info.associatedFeature))
                                        },
                                        onDismiss = {},
                                        dialogRequester = dialogRequester
                                    )
                                    dialogRequester.requestDialog(dialog)
                                } else {
                                    state.navigateTo(FeatureForm(info.associatedFeature))
                                }
                            }
                        )
                    }
                )
                FeatureFormDialog(stateData.stateCollection)
            }
        }
    }
    DisposableEffect(state) {
        onDispose {
            state.setNavController(null)
        }
    }
}

@Composable
private fun FeatureFormLayout(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        title()
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        content()
    }
}

@Composable
private fun FeatureFormTitle(
    title: String,
    subTitle: String,
    hasEdits: Boolean,
    onBackPressed: (() -> Unit)?,
    onClose: (() -> Unit)?,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (onBackPressed != null) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
            Column {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
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
            Spacer(modifier = Modifier.weight(1f))
            if (onClose != null) {
                IconButton(
                    onClick = onClose
                ) {
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
                Button(
                    onClick = onSave,
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onDiscard
                ) {
                    Text(
                        text = "Discard",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FormContent(
    formStateData: FormStateData,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityAssociationFilterClick: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val lazyListState =
        rememberSaveable(inputs = arrayOf(formStateData), saver = LazyListState.Saver) {
            LazyListState()
        }
    // form content
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            //.imePadding()
            .semantics { contentDescription = "lazy column" },
        state = lazyListState
    ) {
        formStateData.stateCollection.forEach { entry ->
            item {
                when (entry.formElement) {
                    is FieldFormElement -> {
                        FieldElement(
                            state = entry.getState<BaseFieldState<*>>(),
                            // set the onClick callback for the field element only if provided
                            onClick = handleFieldFormElementTapAction(
                                fieldFormElement = entry.formElement as FieldFormElement,
                                barcodeTapAction = onBarcodeButtonClick
                            ),
                        )
                    }

                    is GroupFormElement -> {
                        GroupElement(
                            state = entry.getState(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            // set the onClick callback for the group element only if provided
                            onFormElementClick = handleFormElementTapAction(
                                barcodeTapAction = onBarcodeButtonClick
                            )
                        )
                    }

                    is TextFormElement -> {
                        TextFormElement(
                            state = entry.getState(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    }

                    is AttachmentsFormElement -> {
                        AttachmentFormElement(
                            state = entry.getState(),
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    }

                    is UtilityAssociationsFormElement -> {
                        val state = entry.getState<UtilityAssociationsElementState>()
                        UtilityAssociationsElement(
                            state = state,
                            onFilterClick = { index ->
                                onUtilityAssociationFilterClick(state.id, index)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    top = 10.dp,
                                    bottom = 20.dp
                                )
                        )
                    }

                    else -> {
                        // other form elements are not created
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        formStateData.featureForm.hasEdits.collect {
            if (it) {
                val insets = ViewCompat.getRootWindowInsets(view) ?: return@collect
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                with(density) {
                    if (imeVisible) {
                        lazyListState.animateScrollBy(60.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
internal fun InitializingExpressions(
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

/**
 * Creates and remembers state objects for all the supported element types that are part of the
 * provided FeatureForm. These state objects are returned as part of a [FormStateCollection].
 *
 * @param form the [FeatureForm] to create the states for.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [FormStateCollection] created.
 */
@Composable
internal fun rememberStates(
    form: FeatureForm,
    elements: List<FormElement>,
    scope: CoroutineScope
): FormStateCollection {
    val states = MutableFormStateCollection()
    elements.forEach { element ->
        when (element) {
            is FieldFormElement -> {
                val state = rememberFieldState(element, form, scope)
                if (state != null) {
                    states.add(element, state)
                }
            }

            is GroupFormElement -> {
                val stateCollection = rememberStates(
                    form = form,
                    elements = element.elements,
                    scope = scope
                )
                val groupState = rememberBaseGroupState(
                    form = form,
                    groupElement = element,
                    fieldStates = stateCollection
                )
                states.add(element, groupState)
            }

            is TextFormElement -> {
                val state = rememberTextFormElementState(element, form)
                states.add(element, state)
            }

            else -> {}
        }
    }
    // The Toolkit currently only supports AttachmentsFormElements via the
    // default attachments element. Once AttachmentsFormElements can be authored
    // the switch case above should have a case added for AttachmentsFormElement.
    if (form.defaultAttachmentsElement != null) {
        val state = rememberAttachmentElementState(form, form.defaultAttachmentsElement!!)
        states.add(form.defaultAttachmentsElement!!, state)
    }
    return states
}

/**
 * Creates and remembers a [BaseFieldState] for the provided [element].
 *
 * @param element the [FieldFormElement] to create the state for.
 * @param form the [FeatureForm] the [element] is part of.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [BaseFieldState] created.
 */
@Composable
internal fun rememberFieldState(
    element: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): BaseFieldState<out Any?>? {
    return when (element.input) {
        is TextBoxFormInput, is TextAreaFormInput -> {
            val minLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).minLength.toInt()
            } else {
                (element.input as TextAreaFormInput).minLength.toInt()
            }
            val maxLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).maxLength.toInt()
            } else {
                (element.input as TextAreaFormInput).maxLength.toInt()
            }
            rememberFormTextFieldState(
                field = element,
                minLength = minLength,
                maxLength = maxLength,
                form = form,
                scope = scope
            )
        }

        is BarcodeScannerFormInput -> {
            rememberBarcodeTextFieldState(field = element, form = form, scope = scope)
        }

        is DateTimePickerFormInput -> {
            val input = element.input as DateTimePickerFormInput
            rememberDateTimeFieldState(
                field = element,
                minEpochMillis = input.min,
                maxEpochMillis = input.max,
                shouldShowTime = input.includeTime,
                form = form,
                scope = scope
            )
        }

        is ComboBoxFormInput -> {
            rememberComboBoxFieldState(
                field = element,
                form = form,
                scope = scope
            )
        }

        is SwitchFormInput -> {
            rememberSwitchFieldState(
                field = element,
                form = form,
                scope = scope,
                noValueString = stringResource(R.string.no_value)
            )
        }

        is RadioButtonsFormInput -> {
            rememberRadioButtonFieldState(
                field = element,
                form = form,
                scope = scope
            )
        }

        else -> {
            null
        }
    }
}

@Composable
internal fun rememberNavController(vararg inputs: Any): NavHostController {
    val context = LocalContext.current
    rememberNavController()
    return rememberSaveable(inputs = inputs, saver = Saver(
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

/**
 * Handles the tap action for a [FormElement] based on the  input type and the provided tap
 * actions.
 *
 * This will potentially handle taps for any input types that provide custom tap actions.
 *
 * @param fieldFormElement the [FieldFormElement] to handle the tap action for.
 * @param barcodeTapAction the action to perform when the barcode accessory of a [FieldFormElement]
 * with a [BarcodeScannerFormInput] is tapped.
 */
private fun handleFieldFormElementTapAction(
    fieldFormElement: FieldFormElement,
    barcodeTapAction: ((FieldFormElement) -> Unit)?
): (() -> Unit)? {
    return when (fieldFormElement.input) {
        is BarcodeScannerFormInput -> {
            barcodeTapAction?.let {
                { it(fieldFormElement) }
            }
        }

        else -> null
    }
}

/**
 * Handles the tap action for a [FormElement] based on the provided tap actions.
 *
 * This will potentially handle taps for any form element types that provide custom tap actions.
 *
 * @param barcodeTapAction the action to perform when the barcode accessory of a [FieldFormElement]
 * with a [BarcodeScannerFormInput] is tapped.
 */
private fun handleFormElementTapAction(
    barcodeTapAction: ((FieldFormElement) -> Unit)?
): ((FormElement) -> Unit)? {
    return when {
        barcodeTapAction != null -> {
            { formElement ->
                if (formElement is FieldFormElement) {
                    handleFieldFormElementTapAction(formElement, barcodeTapAction)?.invoke()
                }
            }
        }

        else -> null
    }
}

private fun createSaveEditsDialogAction(
    featureForm: FeatureForm,
    navigationAction: () -> Unit,
    dialogRequester: DialogRequester
): () -> Unit {
    return {
        if (featureForm.hasEdits.value) {
            val dialog = createSaveEditsDialog(
                featureForm = featureForm,
                navigationAction = navigationAction,
                onDismiss = {},
                dialogRequester = dialogRequester
            )
            dialogRequester.requestDialog(dialog)
        } else {
            navigationAction()
        }
    }
}

private fun createSaveEditsDialog(
    featureForm: FeatureForm,
    navigationAction: () -> Unit,
    onDismiss: () -> Unit,
    dialogRequester: DialogRequester,
): DialogType.SaveFeatureDialog {
    return DialogType.SaveFeatureDialog(
        onSave = {
            if (featureForm.validationErrors.value.isNotEmpty()) {
                val count = featureForm.validationErrors.value.size
                showValidationErrorsDialog(
                    onDismiss = onDismiss,
                    count = count,
                    dialogRequester = dialogRequester
                )
            } else {
                featureForm.finishEditing().onFailure {
                    val errorDialog = DialogType.ValidationErrorsDialog(
                        onDismiss = onDismiss,
                        title = "Error saving Form",
                        body = it.localizedMessage ?: "An error occurred while saving the form."
                    )
                    dialogRequester.requestDialog(errorDialog)
                }.onSuccess {
                    navigationAction()
                }
            }
        },
        onDiscard = {
            featureForm.discardEdits()
            navigationAction()
        }
    )
}

private fun showValidationErrorsDialog(
    onDismiss: () -> Unit,
    count: Int,
    dialogRequester: DialogRequester,
) {
    val errorDialog = DialogType.ValidationErrorsDialog(
        onDismiss = onDismiss,
        title = "The form has errors",
        body = "$count errors were found in the form. Please correct them before saving."
    )
    dialogRequester.requestDialog(errorDialog)
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormTitlePreview() {
    FeatureFormTitle(
        title = "Feature Form",
        subTitle = "Edit feature attributes",
        hasEdits = true,
        onBackPressed = null,
        onClose = {},
        onSave = {},
        onDiscard = {},
        modifier = Modifier.padding(8.dp)
    )
}
