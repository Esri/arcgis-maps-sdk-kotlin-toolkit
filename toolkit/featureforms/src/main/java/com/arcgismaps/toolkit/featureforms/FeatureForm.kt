package com.arcgismaps.toolkit.featureforms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.formInfoJson
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
    val feature by featureFormState.feature.collectAsState()
    val layer by featureFormState.layer.collectAsState()
    val featureFormDefinition = layer?.formInfoJson?.let {
        FeatureFormDefinition.fromJsonOrNull(it)
    }


    if (featureFormDefinition != null) {
        FeatureFormContent(formDefinition = featureFormDefinition, modifier = modifier)
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Text(text = "No information to display.")
        }
    }
}

@Composable
internal fun FeatureFormContent(
    formDefinition: FeatureFormDefinition,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // title
        Text(text = formDefinition.title, style = TextStyle(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(15.dp))
        Divider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        // form content
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(formDefinition.formElements) { formElement ->
                if (formElement is FieldFeatureFormElement) {
                    FieldElement(field = formElement)
                }
            }
        }
    }
}
