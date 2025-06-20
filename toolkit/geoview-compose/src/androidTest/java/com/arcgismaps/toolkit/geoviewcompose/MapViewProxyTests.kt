/*
 *
 *  Copyright 2025 Esri
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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the [MapViewProxy] class.
 *
 * @since 200.8.0
 */
class MapViewProxyTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * GIVEN a [MapViewProxy] is not linked to a [GeometryEditor]
     * WHEN identifyGeometryEditor is called
     * THEN identify errors.
     *
     * @since 200.8.0
     */
    @Test
    fun testIdentifyGeometryEditorErrorResult() {

        val mapViewProxy = setupMapView(geometryEditor = null)
        runTest {
            val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0, 0.0), 15.dp)
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }
    }

    /**
     * GIVEN a [MapViewProxy] linked to a started and empty [GeometryEditor]
     * WHEN identifyGeometryEditor
     * THEN the identify result is empty.
     *
     * @since 200.8.0
     */
    @Test
    fun testIdentifyGeometryEditorEmptyResult() {

        val geometryEditor = GeometryEditor()
        geometryEditor.start(GeometryType.Point)
        val mapViewProxy = setupMapView(geometryEditor)

        runTest {
            val result = mapViewProxy.identifyGeometryEditor(ScreenCoordinate(0.0, 0.0), 15.dp)
            assertTrue(result.isSuccess)
            assertNull(result.exceptionOrNull())
        }
    }

    private fun setupMapView(geometryEditor: GeometryEditor?): MapViewProxy {
        val mapViewProxy = MapViewProxy()
        val arcGISMap = ArcGISMap(SpatialReference.wgs84())

        composeTestRule.setContent {
            MapView(
                arcGISMap,
                geometryEditor = geometryEditor,
                mapViewProxy = mapViewProxy
            )
        }
        return mapViewProxy
    }
}
