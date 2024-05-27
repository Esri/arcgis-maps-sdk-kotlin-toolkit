/*
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
package com.arcgismaps.toolkit.authentication

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert


/**
 * Waits for the View with the matching package to be visible. Throws an error if the view can't be
 * found.
 *
 * @param packageId the view's package Id to wait for.
 * @since 200.5.0
 */
fun UiDevice.awaitViewVisible(packageId: String) {
    wait(
        Until.findObject(By.pkg(packageId)),
        10_000
    ) ?: run {
        dumpWindowHierarchy(System.err)
        Assert.fail(
            "Could not find the package: ${packageId} on the screen after 10,000 milliseconds." +
                    " Use `UiDevice.dumpWindowHierarchy` to see what's on the screen."
        )
    }
}

/**
 * Enters the [username] and [password] in the OAuth login page.
 *
 * @since 200.5.0
 */
fun UiDevice.enterCredentialsOnBrowser(username: String, password: String, activity: Activity) {
    enterTextByHint(username, "Username")
    closeSoftKeyboard(activity)
    enterTextByHint(password, "Password")
    closeSoftKeyboard(activity)
}

/**
 * Closes the soft keyboard and waits for it to be hidden.
 *
 * @since 200.5.0
 */
fun UiDevice.closeSoftKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus ?: View(activity)
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

    // Wait for the keyboard to be hidden
    wait(Until.gone(By.clazz("android.inputmethodservice.SoftInputWindow")), 5000)
}

/**
 * Enters the [text] in the box with the passed in [hint].
 *
 * @since 200.5.0
 */
fun UiDevice.enterTextByHint(text: String, hint: String) {
    val textBox = findObject(By.hint(hint))
    // clicking the text box selects it and allows us to enter text
    textBox.click()
    textBox.wait(Until.selected(true), 5000)
    textBox.text = text
}

/**
 * Clicks the button in the UI with the passed [text].
 *
 * @since 200.5.0
 */
fun UiDevice.clickByText(text: String) =
    findObject(UiSelector().className("android.widget.Button").textContains(text)).click()
