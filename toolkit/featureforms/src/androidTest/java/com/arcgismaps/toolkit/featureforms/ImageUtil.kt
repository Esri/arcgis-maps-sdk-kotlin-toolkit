/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileOutputStream


/**
 * Not used for testing at this point. Useful for verifying what is on the screen when a test runs.
 * Create a bitmap to pass in with
 * `composeTestRule.onNodeWithContentDescription("some description").captureToImage().asAndroidBitmap()`
 */
internal fun saveScreenshot(filename: String, bmp: Bitmap) {
    val path = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalPath
    FileOutputStream("$path/$filename.png").use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    println("Saved screenshot to $path/$filename.png")
}
