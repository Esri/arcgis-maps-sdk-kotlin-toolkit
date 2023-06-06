/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import kotlinx.coroutines.flow.MutableStateFlow

public interface FeatureFormViewModelInterface {
    public val feature: MutableStateFlow<ArcGISFeature?>
    public val visible: MutableStateFlow<Boolean>
}

/**
 * A view model for the FeatureForms MapView UI
 */
@OptIn(ExperimentalMaterial3Api::class)
public class FeatureFormViewModelImpl() : ViewModel(), FeatureFormViewModelInterface {
    override val feature: MutableStateFlow<ArcGISFeature?> = MutableStateFlow(null)
    override val visible: MutableStateFlow<Boolean> = MutableStateFlow(false)
}

public class FeatureFormViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormViewModelImpl() as T
    }
}
