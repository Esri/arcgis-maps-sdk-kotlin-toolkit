/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms

import android.content.Context
import android.database.CursorWindow
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.arcgismaps.ArcGISEnvironment
import com.esri.logging.Logger
import com.esri.mockingjay.MockingJay
import com.esri.mockingjay.MockingJayConfiguration
import com.esri.mockingjay.MockingJayNetworkInterceptorImpl
import com.esri.mockingjay.MockingJayNetworkObserverImpl
import org.junit.runner.Description
import kotlinx.serialization.json.JsonPrimitive
import org.junit.rules.TestWatcher
import java.io.File
import java.lang.reflect.Field
import kotlin.apply
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Mocks the network requests for a test class and replays the recorded responses when the flag
 * [MockingJayConfiguration.Mode.Playback] is set.
 *
 * @see [LiveNetwork] annotation to skip mocking for specific tests.
 *
 * @since 300.0.0
 */
open class NetworkMockTestCase(
    mockMode: MockingJayConfiguration.Mode = MockingJayConfiguration.Mode.Playback
) {
    @get:Rule
    val networkMockingTestWatcher = NetworkMockingTestWatcher(mockMode)
}

/**
 * A JUnit `TestRule` that conditionally applies the MockingJay network recorder and re-player.
 * It skips mocking if the test is annotated with `@LiveNetwork`, allowing live network requests.
 *
 * @since 300.0.0
 */
class NetworkMockingTestWatcher(private val mockMode: MockingJayConfiguration.Mode) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return if (description.getAnnotation(LiveNetwork::class.java) == null) {
            MockingJayTestWatcher(mockMode).apply(base, description)
        } else {
            base
        }
    }
}

/**
 * Annotation to indicate that a test should bypass network mocking and allow live network requests.
 * Useful for tests in a [NetworkMockTestCase] that require direct interaction with live network services.
 *
 * @since 300.0.0
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class LiveNetwork


/**
 * A JUnit TestWatcher that sets up and tears down the MockingJay network recorder and re-player.
 *
 * @since 300.0.0
 */
class MockingJayTestWatcher(
    private val mode: MockingJayConfiguration.Mode = MockingJayConfiguration.Mode.Playback
) : TestWatcher() {
    private val context: Context
        get() = getInstrumentation().targetContext.applicationContext

    private val testDataPath: String =
        "${Environment.getExternalStorageDirectory()}/Data/androidKotlin"


    override fun starting(description: Description) {
        val testFolder = description.testClass.name.replace(".", "/")
        Logger.log("----------- Starting test: ${description.methodName} -------------")
        super.starting(description)
        ArcGISEnvironment.applicationContext = context

        // Configure the ArcGIS HTTP client to use the MockingJay network re-player and recorder
        ArcGISEnvironment.configureArcGISHttpClient {
            interceptor(MockingJayNetworkInterceptorImpl.shared.interceptor)
            interceptor(MockingJayNetworkObserverImpl.shared.interceptor)
        }

        setupMockingJay(description.methodName, testFolder)

        // Increase the CursorWindow size to 100MB to store bigger files in SQL Lite
        try {
            val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024) //the 100MB is the new size
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun finished(description: Description) {
        val testFolder = description.testClass.name.replace(".", "/")
        teardownMockingJay(description.methodName, testFolder)
    }

    /**
     * Sets up the MockingJay session and configures the ArcGIS HTTP client.
     *
     * @param databaseName The name of the database, obtained from the test name.
     * @param folder The folder where the test data is located, obtained from the test package.
     */
    private fun setupMockingJay(databaseName: String, folder: String) {
        val testName = "$databaseName.db"

        if (mode == MockingJayConfiguration.Mode.Playback) {
            copyDatabaseForTest(testName, "$testDataPath/test-data/mockingjay/$folder")
        }
        // Set the epoch to a distant future date to avoid token expiration issues
        val distantFutureEpoch = JsonPrimitive(80808080808080)
        // Configure the MockingJay session
        val mockingJayConfiguration =
            MockingJayConfiguration(testName, mode).apply {

                // headers and query parameters to remove from the recorded requests
                headersToRemove.add("User-Agent")

                headerReplacements["X-Esri-Authorization"] = "Bearer dummyToken"

                queryParametersToRemove.add("edits")

                queryParameterReplacements["v"] = -1
                queryParameterReplacements["token"] = "dummyToken"
                queryParameterReplacements["code"] = "dummyCode"
                queryParameterReplacements["code_verifier"] = "dummyCodeVerifier"
                queryParameterReplacements["username"] = "dummyUsername"
                queryParameterReplacements["password"] = "dummyPassword"
                queryParameterReplacements.putAll(queryParameterReplacements)

                jsonResponseKeyValueReplacements["access_token"] = JsonPrimitive("accessDummyToken")
                jsonResponseKeyValueReplacements["refresh_token"] =
                    JsonPrimitive("refreshDummyToken")
                jsonResponseKeyValueReplacements["refresh_token_expires_in"] = distantFutureEpoch
                jsonResponseKeyValueReplacements["token"] = JsonPrimitive("dummyToken")
                jsonResponseKeyValueReplacements["expires"] = distantFutureEpoch
                jsonResponseKeyValueReplacements["expires-in"] = distantFutureEpoch

                authenticationTokenParameterKeys.add("token")
                authenticationHeaderParameterKeys.add("X-Esri-Authorization")
            }

        MockingJay.startSession(mockingJayConfiguration, context)
    }

    /**
     * Copies the database file from the test folder to the Android database path.
     */
    private fun copyDatabaseForTest(testDBName: String, testFolderSrc: String) {
        val testDatabase = context.getDatabasePath(testDBName)
        if (testDatabase.exists()) {
            testDatabase.delete()
        }

        val src = File(testFolderSrc, testDBName)
        if (!src.exists()) {
            Logger.log("Database file $testDBName does not exist in $testFolderSrc")
        } else {
            src.copyTo(testDatabase, overwrite = true)
        }
    }

    /**
     * Copies the database file from the Android database path to the specified test folder path.
     */
    private fun copyDatabaseToPath(testDBName: String, testFolderPath: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return
        }
        val dbFile = context.getDatabasePath(testDBName)
        if (dbFile.exists()) {
            Log.e("TAG", "copyDatabaseToPath: ${dbFile.path}")
            val destination = File(testFolderPath, testDBName)
            dbFile.copyTo(destination, overwrite = true)
        } else {
            Logger.log("Database file $testDBName does not exist")
        }
    }

    /**
     * Cleans up the MockingJay session and closes the ArcGIS HTTP client.
     */
    private fun teardownMockingJay(name: String, testFolderPath: String) {
        endSession(name, testFolderPath)
        ArcGISEnvironment.configureArcGISHttpClient()
        Logger.log("----------- Finished test: $name -------------")
    }

    /**
     * Ends the MockingJay session and copies the database file to the specified test folder path.
     */
    private fun endSession(name: String, testFolderPath: String) {
        val testName = "$name.db"

        MockingJay.endSession()
        if (mode == MockingJayConfiguration.Mode.Playback) {
            removeDatabase(testName)
        } else {
            copyDatabaseToPath(testName, "$testDataPath/mocked-responses/$testFolderPath")
        }
    }

    /**
     * Removes the database file from the Android database path.
     */
    private fun removeDatabase(testDBName: String) {
        val testDatabase = context.getDatabasePath(testDBName)
        if (testDatabase.exists()) {
            testDatabase.delete()
        }
    }
}
