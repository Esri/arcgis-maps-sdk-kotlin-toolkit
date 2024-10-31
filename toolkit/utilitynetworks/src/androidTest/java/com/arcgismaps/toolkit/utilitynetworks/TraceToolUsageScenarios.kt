package com.arcgismaps.toolkit.utilitynetworks

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

class TraceToolUsageScenarios {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MapViewWithTraceInBottomSheet(
        arcGISMap: ArcGISMap,
        mapViewProxy: MapViewProxy,
        graphicsOverlay: GraphicsOverlay,
        trace: @Composable () -> Unit
    ) {

//        val scaffoldState = rememberBottomSheetScaffoldState(
//            bottomSheetState = rememberStandardBottomSheetState(
//                initialValue = SheetValue.Expanded,
//                skipHiddenState = true
//            )
//        )
//
//        BottomSheetScaffold(
//            sheetContent = {
//                AnimatedVisibility(
//                    visible = true,
//                    enter = slideInVertically { h -> h },
//                    exit = slideOutVertically { h -> h },
//                    label = "trace tool",
//                    modifier = Modifier.heightIn(min = 0.dp, max = 350.dp)
//                ) {
//                    trace()
////                    Trace(viewModel.traceState)
//                }
//            },
//            modifier = Modifier.fillMaxSize(),
//            scaffoldState = scaffoldState,
//            sheetPeekHeight = 100.dp,
//            sheetSwipeEnabled = true,
//            topBar = null
//        ) { padding ->
        Column {
            MapView(
                arcGISMap = arcGISMap,
                mapViewProxy = mapViewProxy,
                graphicsOverlays = listOf(graphicsOverlay),
                modifier = Modifier
//                    .padding(padding)
                    .weight(1f)
                    .fillMaxSize(),
//                onSingleTapConfirmed = { singleTapConfirmedEvent ->
//                    Log.i("MainScreen -", "${singleTapConfirmedEvent.mapPoint?.x}, ${singleTapConfirmedEvent.mapPoint?.y}, ${singleTapConfirmedEvent.mapPoint?.spatialReference?.wkid}")
//                    singleTapConfirmedEvent.mapPoint?.let {
//                        coroutineScope.launch {
//                            viewModel.traceState.addStartingPoint(it)
//                        }
//                    }
//                }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                trace()
            }
        }
    }

}