/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.geoviewcompose

import android.os.Parcel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Card
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor

/**
 * Tests for the [ViewpointPersistence] class.
 *
 * @since 200.4.0
 */
class MapViewProxyTests {

        @get:Rule
        val composeTestRule = createComposeRule()
    /**
     * GIVEN a [ViewpointPersistence] object
     * WHEN it is created from a [Parcel]
     * THEN the read back object is equal to the original one
     *
     * @since 200.4.0
     */
    @Test
    fun testIdentifyGeometryEditorErrorResult() = runTest {
        // Create instances of each class
        val mapViewProxy = MapViewProxy()
        val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0,0.0), 15.dp)

        // Assert that the read back instances are equal to the original ones
        assertTrue(result.isFailure)
    }

    @Test
    fun testIdentifyGeometryEditorEmptyResult() = runTest {

        val mapViewProxy = MapViewProxy()


        //mapView.geometryEditor = geometryEditor

        composeTestRule.setContent {
            val context = LocalContext.current.applicationContext
            val mapView = remember { MapView(context) }
            val geometryEditor = remember {GeometryEditor()}
            mapView.geometryEditor = geometryEditor
//            val mapView by rememberUpdatedState() {
//            //val mapView = remember {
//                //MapView(LocalContext.current.applicationContext)
//                context =
//                geometryEditor = geometryEditor
//            }

            mapViewProxy.setMapView(mapView)

            geometryEditor.start(GeometryType.Point)

//            context.applicationContext.run {
//                val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0, 0.0), 15.dp)
//                assertTrue(result.isSuccess)
//            }
        }

//        composeTestRule.run {
            val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0, 0.0), 15.dp)
                   assertTrue(result.isSuccess)
//        }
//        runTest {
//            val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0, 0.0), 15.dp)
//                   assertTrue(result.isSuccess)
//        }

        //composeTestRule.waitUntil {mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0,0.0).isSuccess  }


}

}
