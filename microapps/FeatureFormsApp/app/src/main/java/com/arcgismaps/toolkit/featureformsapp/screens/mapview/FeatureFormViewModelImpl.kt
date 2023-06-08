import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.FeatureFormViewModelInterface
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A view model for the FeatureForms MapView UI
 */
class FeatureFormViewModelImpl : ViewModel(), FeatureFormViewModelInterface {
    override val feature: MutableStateFlow<ArcGISFeature?> = MutableStateFlow(null)
    override val visible: MutableStateFlow<Boolean> = MutableStateFlow(false)
}

class FeatureFormViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormViewModelImpl() as T
    }
}
