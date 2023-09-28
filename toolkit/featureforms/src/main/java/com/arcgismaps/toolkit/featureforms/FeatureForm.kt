package com.arcgismaps.toolkit.featureforms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.components.FieldElement

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
    var initialEvaluation by remember(featureForm) { mutableStateOf(false) }
    LaunchedEffect(featureForm) {
        // ensure expressions are evaluated before state objects are created.
        featureForm?.evaluateExpressions()
        initialEvaluation = true
    }
    
    featureForm?.let {
        if (initialEvaluation) {
            FeatureFormContent(form = it, modifier = modifier)
        } else {
            InitializingExpressions(modifier)
        }
    } ?: run {
        NoDataToDisplay(modifier)
    }
}

@Composable
internal fun InitializingExpressions(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(modifier = Modifier
            .width(80.dp)
            .height(80.dp)
        )
        Text(text = "Initializing")
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
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(form.elements) { formElement ->
                if (formElement is FieldFormElement) {
                    FieldElement(
                        field = formElement,
                        form = form
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun InitializingExpressionsPreview() {
    InitializingExpressions()
}


@Preview
@Composable
private fun NoDataPreview() {
    NoDataToDisplay()
}
