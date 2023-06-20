package com.arcgismaps.toolkit.featureformsapp.screens.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.toolkit.featureforms.FeatureFormStateImpl
import com.arcgismaps.toolkit.featureforms.FeatureFormState

class FormViewModel : ViewModel(), FeatureFormState by FeatureFormStateImpl()

class FormViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FormViewModel() as T
    }
}
