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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.internal.components.dialogs.SaveEditsDialog
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import kotlinx.coroutines.launch

/**
 * Composable function that displays the feature form screen.
 *
 * @param formStateData The form state data.
 * @param showFormActions A boolean value that indicates whether to show the form actions.
 * @param showCloseIcon A boolean value that indicates whether to show the close icon.
 * @param hasBackStack A boolean value that indicates whether there is a back stack.
 * @param isEvaluatingExpressions A boolean value that indicates whether the expressions are being evaluated.
 * @param onClose The callback to be invoked when the close icon is clicked. This is only invoked
 * when there are no edits in the form. If there are edits, this callback is invoked after a successful
 * save or discard operation.
 * @param onSave The callback to be invoked when the save button is clicked. The parameter indicates
 * whether this action should be followed by a back navigation. The callback should return a [Result] that
 * indicates the success or failure of the save operation.
 * @param onDiscard The callback to be invoked when the discard button is clicked. The parameter indicates
 * whether this action should be followed by a back navigation.
 * @param onNavigateBack The callback to be invoked when the back navigation is requested.
 * @param onBarcodeButtonClick The callback to be invoked when the barcode button is clicked.
 * @param onUtilityFilterSelected The callback to be invoked when the utility filter is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun FeatureFormScreen(
    formStateData: FormStateData,
    hasBackStack: Boolean,
    isEvaluatingExpressions: Boolean,
    onClose: () -> Unit,
    onSave: suspend (Boolean) -> Result<Unit>,
    onDiscard: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityFilterSelected: (UtilityAssociationsElementState) -> Unit,
    modifier: Modifier = Modifier
) {
    val featureForm = formStateData.featureForm
    val states = formStateData.stateCollection
    val hasEdits by featureForm.hasEdits.collectAsState()
    var showDiscardEditsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingCloseAction by rememberSaveable {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()
    val onBackAction: () -> Unit = {
        if (hasEdits) {
            showDiscardEditsDialog = true
        } else {
            if (hasBackStack) {
                // Navigate back to the previous form if there are no edits
                onNavigateBack()
            }
        }
    }

//    FeatureFormLayout(
//        modifier = modifier,
//        title = {
//            val title by featureForm.title.collectAsState()
//            FeatureFormTitle(
//                title = title,
//                subTitle = featureForm.description,
//                hasEdits = if (showFormActions) hasEdits else false,
//                showCloseIcon = showCloseIcon,
//                modifier = Modifier
//                    .padding(
//                        vertical = 8.dp,
//                        horizontal = if (hasBackStack) 8.dp else 16.dp
//                    )
//                    .fillMaxWidth(),
//                onBackPressed = if (hasBackStack) onBackAction else null,
//                onClose = {
//                    if (hasEdits) {
//                        pendingCloseAction = true
//                        showDiscardEditsDialog = true
//                    } else {
//                        onClose()
//                    }
//                },
//                onSave = {
//                    scope.launch {
//                        onSave(false)
//                    }
//                },
//                onDiscard = {
//                    onDiscard(false)
//                }
//            )
//            InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
//                isEvaluatingExpressions
//            }
//        },
//        content = {
//
//        }
//    )
    FormContent(
        formStateData = formStateData,
        onBarcodeButtonClick = onBarcodeButtonClick,
        onUtilityAssociationFilterClick = onUtilityFilterSelected,
        modifier = modifier
    )
    if (showDiscardEditsDialog) {
        SaveEditsDialog(
            onDismissRequest = {
                showDiscardEditsDialog = false
                pendingCloseAction = false
            },
            onSave = {
                scope.launch {
                    val willNavigate = !pendingCloseAction
                    onSave(willNavigate).onSuccess {
                        // Run the pending close action if there is one
                        if (pendingCloseAction) {
                            onClose()
                            pendingCloseAction = false
                        }
                    }
                }
                showDiscardEditsDialog = false
            },
            onDiscard = {
                val willNavigate = !pendingCloseAction
                onDiscard(willNavigate)
                // Run the pending close action if there is one
                if (pendingCloseAction) {
                    onClose()
                    pendingCloseAction = false
                }
                showDiscardEditsDialog = false
            }
        )
    }
    // only enable back navigation if there is a previous route
    BackHandler(hasBackStack) {
        onBackAction()
    }
    FeatureFormDialog(states)
}

@Composable
private fun FormContent(
    formStateData: FormStateData,
    modifier: Modifier = Modifier,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityAssociationFilterClick: (UtilityAssociationsElementState) -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val lazyListState = rememberSaveable(
        inputs = arrayOf(formStateData),
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }
    // form content
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                            onItemClick = { selected ->
                                state.setSelectedFilterResult(selected)
                                onUtilityAssociationFilterClick(state)
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
