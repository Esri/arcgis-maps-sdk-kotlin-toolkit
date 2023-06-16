package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.toolkit.featureforms.FeatureFormImpl
import com.arcgismaps.toolkit.featureforms.FeatureFormInterface

class FormViewModel : ViewModel(), FeatureFormInterface by FeatureFormImpl()

class FormViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FormViewModel() as T
    }
}
