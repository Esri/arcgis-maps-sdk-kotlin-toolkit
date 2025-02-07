package com.arcgismaps.toolkit.legend.internal

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.LayerContent
import com.arcgismaps.mapping.layers.LegendInfo
import com.arcgismaps.mapping.view.GeoView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal data class LegendProperties(
    val geoModelLayersInOrder: SnapshotStateList<Layer>,
    val legendContent: SnapshotStateList<Any>,
    val legendInfos: SnapshotStateMap<LayerContent, LegendInfo>
) {
    companion object {
        val NOT_INITIALIZED = LegendProperties(
            geoModelLayersInOrder = mutableStateListOf(),
            legendContent = mutableStateListOf(),
            legendInfos = mutableStateMapOf<LayerContent, LegendInfo>()
        )
    }
}

internal fun getGeoModelLayersInOrder(
    geoModel: GeoModel,
    reverseLayerOrder: Boolean
): SnapshotStateList<Layer> {

    var basemap: Basemap?
    var reversedLayerList = mutableListOf<Layer>()
    var layerListToDisplayInLegend = mutableStateListOf<Layer>()

    when (geoModel) {
        is ArcGISMap -> {
            basemap = geoModel.basemap.value
            geoModel.operationalLayers.let { reversedLayerList.addAll(it) }
        }

        is ArcGISScene -> {
            basemap = geoModel.basemap.value
            geoModel.operationalLayers.let { reversedLayerList.addAll(it) }
        }
    }

    basemap?.let { it ->
        it.referenceLayers.let { reversedLayerList.addAll(it) }
        it.baseLayers.let { reversedLayerList.addAll(0, it) }
    }

    reversedLayerList = reversedLayerList.filter { it.isVisible && it.showInLegend }.toMutableList()

    if (!reverseLayerOrder && reversedLayerList.isNotEmpty()) {
        layerListToDisplayInLegend.addAll(reversedLayerList.reversed())
    } else {
        layerListToDisplayInLegend.addAll(reversedLayerList)
    }

    return layerListToDisplayInLegend
}

internal suspend fun loadLayers(layerList: MutableList<Layer>, legendInfoMap: SnapshotStateMap<LayerContent, List<LegendInfo>>) {
    layerList.forEach { loadIndividualLayer(it, legendInfoMap) }
}

private suspend fun loadIndividualLayer(layerContent: LayerContent, legendInfoMap: SnapshotStateMap<LayerContent, List<LegendInfo>>) {
    if (layerContent is Layer) {
        layerContent.load().onSuccess {
            loadSublayersOrLegendInfos(layerContent, legendInfoMap)
        }
    } else {
        loadSublayersOrLegendInfos(layerContent, legendInfoMap)
    }
}

private suspend fun loadSublayersOrLegendInfos(layerContent: LayerContent, legendInfoMap: SnapshotStateMap<LayerContent, List<LegendInfo>>) {
    // TODO: Handle when subLayerContent changes and update legend accordingly
    // This is the deepest level we can go and we're assured that
    // the AGSLayer is loaded for this layer/sublayer, so
    // set the contents changed handler.
//    layerContent.subLayerContents.collect {
//     TODO: Handle when subLayerContent changes and update legend accordingly
//    }
    //    layerContent.subLayerContentsChangedHandler = {
//        Handler(Looper.getMainLooper()).post {
//            updateLegendArray()
//        }
//    }
    setCollectOnSubLayerContents(layerContent)

    if (layerContent.subLayerContents.value.isNotEmpty()) {
        layerContent.subLayerContents.value.forEach { loadIndividualLayer(it, legendInfoMap) }
    } else {
        layerContent.fetchLegendInfos().onSuccess {
            legendInfoMap[layerContent] = it
        }
    }
}

internal fun generateLegendContent(
    legendContent: SnapshotStateList<Any>,
    geoModelLayersInOrder: SnapshotStateList<Layer>,
    legendInfos: SnapshotStateMap<LayerContent, List<LegendInfo>>
) {
    legendContent.clear()
//    legendInfos.forEach { (layerContent, legendInfo) ->
//        if (legendInfo.isNotEmpty()) {
//            legendContent.add(legendInfo)
//        }
//    }

}

private suspend fun setCollectOnSubLayerContents(layerContent: LayerContent) {
//    layerContent.subLayerContents.collect {
//        // TODO: Handle when subLayerContent changes and update legend accordingly
//    }
    layerContent.subLayerContents.onEach {
        Log.e("MainScreen **", "SubLayerContents: layername: ${layerContent.name} isVisible - ${layerContent.isVisible}" )
    }
        .launchIn(CoroutineScope(Dispatchers.Main))
}


/**
 * Returns [this] Result, but if it is a failure with the specified exception type, then it throws the exception.
 *
 * @param T a [Throwable] type which should be thrown instead of encapsulated in the [Result].
 */
internal inline fun <reified T : Throwable, R> Result<R>.except(): Result<R> =
    onFailure { if (it is T) throw it }

/**
 * Runs the specified [block] with [this] value as its receiver and catches any exceptions, returning a `Result` with the
 * result of the block or the exception. If the exception is a [CancellationException], the exception will not be encapsulated
 * in the failure but will be rethrown.
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> =
    runCatching(block)
        .except<CancellationException, R>()

