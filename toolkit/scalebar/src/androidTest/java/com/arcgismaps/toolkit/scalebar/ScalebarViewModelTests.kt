package com.arcgismaps.toolkit.scalebar

import com.arcgismaps.toolkit.scalebar.internal.ScalebarViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScalebarViewModelTests {

    @Test
    fun testImageAttachments() = runTest {
        val viewModel: ScalebarViewModel = ScalebarViewModel()
//        viewModel.updateScaleBar(1.0, null, ScalebarStyle.DOUBLE_LINE)
    }
}