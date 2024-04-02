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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseGroupState
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
import com.arcgismaps.toolkit.featureforms.internal.components.text.rememberFormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.utils.FeatureFormDialog
import kotlinx.coroutines.CoroutineScope

/**
 * The "property" determines the behavior of when the validation errors are visible.
 */
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

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using forms that have been configured externally. Forms may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)).
 *
 * Note : Even though the [FeatureForm] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param featureForm The [FeatureForm] configuration.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * FeatureForm.
 * @param validationErrorVisibility The [ValidationErrorVisibility] that determines the behavior of
 * when the validation errors are visible. Default is [ValidationErrorVisibility.Automatic] which
 * indicates errors are only visible once the respective field gains focus.
 *
 * @since 200.4.0
 */
@Composable
public fun FeatureForm(
    featureForm: FeatureForm,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
) {
    val stateData = remember(featureForm) {
        StateData(featureForm)
    }
    FeatureForm(
        stateData = stateData,
        modifier = modifier,
        validationErrorVisibility = validationErrorVisibility
    )
}

/**
 * A wrapper to hold state data. This provides a [Stable] class to enable smart recompositions,
 * since [FeatureForm] is not stable.
 */
@Immutable
internal data class StateData(@Stable val featureForm: FeatureForm)

/**
 * This composable uses the [StateData] class to display a [FeatureForm].
 */
@Composable
internal fun FeatureForm(
    stateData: StateData,
    modifier: Modifier = Modifier,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
) {
    val featureForm = stateData.featureForm
    val scope = rememberCoroutineScope()
    val states = rememberStates(form = featureForm, scope = scope)
    FeatureFormBody(form = featureForm, states = states, modifier = modifier)
    FeatureFormDialog()
    // launch a new side effect in a launched effect when validationErrorVisibility changes
    LaunchedEffect(validationErrorVisibility) {
        // if it set to always show errors force each field to validate itself and show any errors
        if (validationErrorVisibility == ValidationErrorVisibility.Visible) {
            states.forEach { entry ->
                // validate all fields
                if (entry.formElement is FieldFormElement) {
                    entry.getState<BaseFieldState<*>>().forceValidation()
                }
                // validate any fields that are within a group
                if (entry.formElement is GroupFormElement) {
                    entry.getState<BaseGroupState>().fieldStates.forEach { childEntry ->
                        childEntry.getState<BaseFieldState<*>>().forceValidation()
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureFormTitle(featureForm: FeatureForm, modifier: Modifier = Modifier) {
    val title by featureForm.title.collectAsState()
    Text(
        text = title,
        style = TextStyle(fontWeight = FontWeight.Bold),
        modifier = modifier
    )
}

@Composable
private fun FeatureFormBody(
    form: FeatureForm,
    states: FormStateCollection,
    modifier: Modifier = Modifier
) {
    var initialEvaluation by rememberSaveable(form) { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // title
        FeatureFormTitle(
            featureForm = form,
            modifier = Modifier.padding(horizontal = 15.dp)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp)
        )
        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
            initialEvaluation
        }
        Divider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        // form content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "lazy column" },
            state = lazyListState
        ) {
            states.forEach { entry ->
                item {
                    when (entry.formElement) {
                        is FieldFormElement -> {
                            FieldElement(state = entry.getState<BaseFieldState<*>>())
                        }

                        is GroupFormElement -> {
                            GroupElement(
                                state = entry.getState(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp, vertical = 10.dp)
                            )
                        }

                        else -> {
                            // other form elements are not created
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(form) {
        // ensure expressions are evaluated
        form.evaluateExpressions()
        initialEvaluation = true
    }
}

@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 0f else 1f,
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
    scope: CoroutineScope
): FormStateCollection {
    val states = MutableFormStateCollection()
    form.elements.forEach { element ->
        when (element) {
            is FieldFormElement -> {
                val state = rememberFieldState(element, form, scope)
                if (state != null) {
                    states.add(element, state)
                }
            }

            is GroupFormElement -> {
                val fieldStateCollection = MutableFormStateCollection()
                element.elements.forEach {
                    if (it is FieldFormElement) {
                        val state = rememberFieldState(
                            element = it,
                            form = form,
                            scope = scope
                        )
                        if (state != null) {
                            fieldStateCollection.add(it, state)
                        }
                    }
                }
                val groupState = rememberBaseGroupState(
                    form = form,
                    groupElement = element,
                    fieldStates = fieldStateCollection
                )
                states.add(element, groupState)
            }

            else -> {}
        }
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
