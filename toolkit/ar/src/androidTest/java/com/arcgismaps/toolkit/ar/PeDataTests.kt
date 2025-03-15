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

class PeDataTests {
    var initialPeDirectory: String? = null
    val gridFile = File(
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

    @Test
    fun configurePeData() = runTest {
        assertThat(TransformationCatalog.projectionEngineDirectory).isEmpty()
        assertThat(gridFile.exists()).isFalse()

        val context = InstrumentationRegistry.getInstrumentation().context
        assertThat(PeData.configure(context).isSuccess).isTrue()
        assertThat(gridFile.exists()).isTrue()
        assertThat(gridFile.isFile).isTrue()

        // call configure again, to check that it returns success also when PE data
        // have already been configured
        assertThat(PeData.configure(context).isSuccess).isTrue()
        assertThat(gridFile.exists()).isTrue()
        assertThat(gridFile.isFile).isTrue()
    }

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