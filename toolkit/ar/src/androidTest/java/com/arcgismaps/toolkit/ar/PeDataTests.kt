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

package com.arcgismaps.toolkit.ar

import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.geometry.TransformationCatalog
import com.arcgismaps.toolkit.ar.internal.PeData
import com.arcgismaps.toolkit.ar.internal.PeData.PEDATA_FILE_NAME
import com.arcgismaps.toolkit.ar.internal.PeData.PEDATA_ROOT_DIRECTORY
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for deploying PE data as part of [WorldScaleSceneView].
 *
 * @since 200.7.0
 */
class PeDataTests {
    private var initialPeDirectory: String? = null
    private val gridFile = File(
        InstrumentationRegistry.getInstrumentation().context.getExternalFilesDir(null),
        "$PEDATA_ROOT_DIRECTORY/$PEDATA_FILE_NAME"
    )

    @Before
    fun setup() {
        initialPeDirectory = TransformationCatalog.projectionEngineDirectory
        TransformationCatalog.projectionEngineDirectory = ""
        if (gridFile.exists()) {
            gridFile.delete()
        }
    }

    @After
    fun tearDown() {
        // restore initial state
        TransformationCatalog.projectionEngineDirectory = initialPeDirectory
    }

    /**
     * Given a PE data file that is part of the ar library assets,
     * When [PeData.configure] is called,
     * Then the PE data file should be copied to the external files directory.
     *
     * When [PeData.configure] is called again,
     * Then the PE data file should not be copied again, but [PeData.configure] should return success.
     *
     * @since 200.7.0
     */
    @Test
    fun configurePeData() = runTest {
        assertThat(TransformationCatalog.projectionEngineDirectory).isEmpty()
        assertThat(gridFile.exists()).isFalse()

        val context = InstrumentationRegistry.getInstrumentation().context
        assertThat(PeData.configure(context).isSuccess).isTrue()
        assertThat(gridFile.exists()).isTrue()
        assertThat(gridFile.isFile).isTrue()
        assertThat(TransformationCatalog.projectionEngineDirectory).isEqualTo(gridFile.parent)

        // call configure again, to check that it returns success also when PE data
        // have already been configured
        assertThat(PeData.configure(context).isSuccess).isTrue()
        assertThat(gridFile.exists()).isTrue()
        assertThat(gridFile.isFile).isTrue()
        assertThat(TransformationCatalog.projectionEngineDirectory).isEqualTo(gridFile.parent)
    }

    /**
     * Given a PE data file that is part of the ar library assets,
     * When [TransformationCatalog.projectionEngineDirectory] has been set prior to
     * calling [PeData.configure],
     * Then the PE data file is not copied from the assets and the path of
     * [TransformationCatalog.projectionEngineDirectory] remains unchanged.
     *
     * @since 200.7.0
     */
    @Test
    fun configurePeDataPathAlreadySet() = runTest {
        assertThat(TransformationCatalog.projectionEngineDirectory).isEmpty()
        assertThat(gridFile.exists()).isFalse()
        val somePath = InstrumentationRegistry.getInstrumentation().context.getExternalFilesDir(null)?.canonicalPath
        TransformationCatalog.projectionEngineDirectory = somePath

        val context = InstrumentationRegistry.getInstrumentation().context
        assertThat(PeData.configure(context).isSuccess).isTrue()
        assertThat(gridFile.exists()).isFalse()
        assertThat(TransformationCatalog.projectionEngineDirectory).isEqualTo(somePath)
    }
}
