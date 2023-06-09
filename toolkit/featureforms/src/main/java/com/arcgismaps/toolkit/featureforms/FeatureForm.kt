package com.arcgismaps.toolkit.featureforms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
public fun FeatureForm(viewModel: FeatureFormViewModelInterface) {
    val feature by viewModel.feature.collectAsState()
    Column(modifier = Modifier.heightIn(min = 300.dp, max = 500.dp)) {
        Row {
            Text(text = "Row 1")
        }
        Spacer(modifier = Modifier)
        Row {
            Text(text = "Row 2")
        }
        Button(onClick = { viewModel.setFormVisibility(false) }) {
            Text("Close")
        }
    }
}
