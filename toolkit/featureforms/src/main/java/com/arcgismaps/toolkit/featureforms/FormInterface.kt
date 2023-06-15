package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.data.ArcGISFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public interface FormInterface {
    public val feature: StateFlow<ArcGISFeature?>
    public fun setFeature(feature: ArcGISFeature)
}

/**
 * A view model for the FeatureForms MapView UI
 */
public class FormInterfaceImpl : FormInterface {
    private val _feature: MutableStateFlow<ArcGISFeature?> = MutableStateFlow(null)
    override val feature: StateFlow<ArcGISFeature?> = _feature.asStateFlow()

    override fun setFeature(feature: ArcGISFeature) {
        _feature.value = feature
    }
}
