package com.arcgismaps.toolkit.featureforms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.formInfoJson
import com.arcgismaps.toolkit.featureforms.components.FieldElement

@Composable
public fun FeatureForm(
    featureFormState: FeatureFormState,
    modifier: Modifier = Modifier
) {
    val feature by featureFormState.feature.collectAsState()
    val featureTable = feature?.featureTable as ArcGISFeatureTable?
    featureTable?.formInfoJson?.let {
        FeatureFormDefinition.fromJsonOrNull(it)?.let { formDefinition ->
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = formDefinition.title, style = TextStyle(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(15.dp))
                Divider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    items(formDefinition.formElements) { formElement ->
                        if (formElement is FieldFeatureFormElement) {
                            FieldElement(field = formElement)
                        }
                    }
                }
            }
        }
    }
}
