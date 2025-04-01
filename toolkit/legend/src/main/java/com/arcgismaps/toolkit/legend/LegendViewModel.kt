import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.arcgismaps.toolkit.legend.LayerContentData


internal class LegendViewModel: ViewModel() {
    val layerContentData = mutableStateListOf<LayerContentData>()
}