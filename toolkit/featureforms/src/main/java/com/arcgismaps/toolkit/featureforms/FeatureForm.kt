package com.arcgismaps.toolkit.featureforms

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.components.base.rememberBaseGroupState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.rememberCodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.rememberRadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.rememberSwitchFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.rememberDateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.formelement.FieldElement
import com.arcgismaps.toolkit.featureforms.components.formelement.GroupElement
import com.arcgismaps.toolkit.featureforms.components.text.rememberFormTextFieldState
import com.arcgismaps.toolkit.featureforms.utils.DialogType
import com.arcgismaps.toolkit.featureforms.utils.FeatureFormDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.util.Objects

/**
 * A composable Form toolkit component that enables users to edit field values of features in a
 * layer using forms that have been configured externally (using either in the the Web Map Viewer
 * or the Fields Maps web app). The composable uses the [featureFormState] as the UI state.
 *
 * @since 200.2.0
 */
@Composable
public fun FeatureForm(
    featureFormState: FeatureFormState,
    modifier: Modifier = Modifier
) {
    val featureForm by featureFormState.featureForm.collectAsState()
    var initialEvaluation by rememberSaveable(featureForm) { mutableStateOf(false) }

    featureForm?.let {
        InitializingExpressions(modifier) {
            initialEvaluation
        }
        FeatureFormContent(form = it, modifier = modifier)
    } ?: run {
        NoDataToDisplay(modifier)
    }

    LaunchedEffect(featureForm) {
        // ensure expressions are evaluated before state objects are created.
        featureForm?.evaluateExpressions()
        // add an artificial delay of 300ms to avoid the slight flicker if the
        // expressions are evaluated quickly
        delay(300)
        initialEvaluation = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    AnimatedContent(
        targetState = evaluationProvider(),
        transitionSpec = {
            slideInVertically() with
                slideOutVertically(
                    animationSpec = tween()
                ) { 0 } + fadeOut()
        },
        label = "evaluation loading animation"
    ) { evaluated ->
        if (!evaluated) {
            Surface(modifier = modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                    Text(text = "Initializing", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
internal fun NoDataToDisplay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(text = "No information to display.")
    }
}

@Composable
internal fun FeatureFormContent(
    form: FeatureForm,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fieldStateMap = rememberFieldStates(
        form = form,
        elements = form.elements,
        context = context,
        scope = scope
    )
    val groupStateMap = rememberGroupStates(
        form = form,
        context = context,
        scope = scope
    )
    var dialogType: DialogType by rememberSaveable {
        mutableStateOf(DialogType.NoDialog)
    }
    FeatureFormBody(
        form = form,
        fieldStateMap = fieldStateMap,
        groupStateMap = groupStateMap,
        modifier = modifier
    ) { state, id ->
        if (state is DateTimeFieldState) {
            dialogType = DialogType.DatePickerDialog(id)
        } else if (state is CodedValueFieldState) {
            dialogType = DialogType.ComboBoxDialog(id)
        }
    }
    FeatureFormDialog(
        dialogType = dialogType,
        state = dialogType.getStateKey()?.let { stateKey ->
            fieldStateMap[stateKey] ?: groupStateMap.firstNotNullOfOrNull {
                it.value.fieldStates[stateKey]
            }
        },
        onDismissRequest = {
            dialogType = DialogType.NoDialog
        }
    )
}

@Composable
private fun FeatureFormBody(
    form: FeatureForm,
    fieldStateMap: Map<Int, BaseFieldState<*>>,
    groupStateMap: Map<Int, BaseGroupState>,
    modifier: Modifier = Modifier,
    onFieldDialogRequest: ((BaseFieldState<*>, Int) -> Unit)? = null
) {
    val lazyListState = rememberLazyListState()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // title
        Text(text = form.title, style = TextStyle(fontWeight = FontWeight.Bold))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp)
        )
        Divider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        // form content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "lazy column" },
            state = lazyListState
        ) {
            items(form.elements) { formElement ->
                when (formElement) {
                    is FieldFormElement -> {
                        val state = fieldStateMap[formElement.id]
                        if (state != null) {
                            FieldElement(
                                state = state,
                                onDialogRequest = {
                                    onFieldDialogRequest?.invoke(state, formElement.id)
                                }
                            )
                        }
                    }

                    is GroupFormElement -> {
                        val state = groupStateMap[formElement.id]
                        if (state != null) {
                            GroupElement(
                                formElement,
                                state,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 15.dp,
                                        end = 15.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    ),
                                onDialogRequest = { baseFieldState, key ->
                                    onFieldDialogRequest?.invoke(baseFieldState, key)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberGroupStates(
    form: FeatureForm,
    context: Context,
    scope: CoroutineScope,
): Map<Int, BaseGroupState> {
    return form.elements.filterIsInstance<GroupFormElement>().associateBy(
        { groupElement ->
            groupElement.id
        },
        { groupElement ->
            val fieldStates = rememberFieldStates(
                form = form,
                elements = groupElement.formElements,
                context = context,
                scope = scope
            )
            rememberBaseGroupState(groupElement = groupElement, fieldStates = fieldStates)
        }
    )
}

@Composable
internal fun rememberFieldStates(
    form: FeatureForm,
    elements: List<FormElement>,
    context: Context,
    scope: CoroutineScope
): Map<Int, BaseFieldState<*>> {
    val stateMap = mutableMapOf<Int, BaseFieldState<*>>()
    elements.forEach {  element ->
        if (element is FieldFormElement) {
            val state = when (element.input) {
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
                        context = context,
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
                    rememberCodedValueFieldState(
                        field = element,
                        form = form,
                        scope = scope
                    )
                }

                is SwitchFormInput -> {
                    val input = element.input as SwitchFormInput
                    val initialValue = element.formattedValue
                    val fallback = initialValue.isEmpty()
                        || (element.value.value != input.onValue.code && element.value.value != input.offValue.code)
                    rememberSwitchFieldState(
                        field = element,
                        form = form,
                        fallback = fallback,
                        scope = scope,
                        noValueString = context.getString(R.string.no_value)
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
            if (state != null) {
                stateMap[element.id] = state
            }
        }
    }
    return stateMap
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun InitializingExpressionsPreview() {
    InitializingExpressions { false }
}


@Preview
@Composable
private fun NoDataPreview() {
    NoDataToDisplay()
}

/**
 * Unique id for each form element.
 */
internal val FieldFormElement.id: Int
    get() {
        return Objects.hash(fieldName, label, description, hint)
    }

/**
 * Unique id for each form element.
 */
internal val GroupFormElement.id: Int
    get() {
        return Objects.hash(
            formElements.forEach { if (it is FieldFormElement) it.id },
            label,
            description
        )
    }
