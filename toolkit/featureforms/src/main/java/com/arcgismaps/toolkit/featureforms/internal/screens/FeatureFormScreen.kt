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

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.internal.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElement
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog

/**
 * Composable function that displays the feature form screen.
 *
 * @param formStateData The form state data.
 * @param onBarcodeButtonClick The callback to be invoked when the barcode button is clicked.
 * @param onUtilityFilterSelected The callback to be invoked when the utility filter is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun FeatureFormScreen(
    formStateData: FormStateData,
    onBarcodeButtonClick: ((FieldFormElement) -> Unit)?,
    onUtilityFilterSelected: (UtilityAssociationsElementState) -> Unit,
    modifier: Modifier = Modifier
) {
    FormContent(
        formStateData = formStateData,
        onBarcodeButtonClick = onBarcodeButtonClick,
        onUtilityAssociationFilterClick = onUtilityFilterSelected,
        modifier = modifier
    )
    FeatureFormDialog(formStateData.stateCollection)
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
