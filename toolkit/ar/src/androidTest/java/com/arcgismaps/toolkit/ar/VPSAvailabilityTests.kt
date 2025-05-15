package com.arcgismaps.toolkit.ar

import android.Manifest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [WorldScaleSceneViewProxy.checkVpsAvailability]
 */
class VPSAvailabilityTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    /**
     * Tests that VPS is available at a location with data,
     * and unavailable at a location without data.
     *
     * This test should be run manually on a device with [Google Play services for AR](https://play.google.com/store/apps/details?id=com.google.ar.core) installed.
     * A valid [API key](https://developers.google.com/ar/develop/authorization?platform=android) must be placed in the manifest.
     *
     * @since 200.8.0
     */
    @Ignore("Requires ARCore installation + API key")
    @Test
    fun testVPSAvailability() = runTest {
        // check ARCore is installed
        val arCoreAvailability = ArCoreApk.getInstance().checkAvailability(ApplicationProvider.getApplicationContext())
        assertThat(arCoreAvailability).isEqualTo(ArCoreApk.Availability.SUPPORTED_INSTALLED)

        val proxy = WorldScaleSceneViewProxy()

        // crossroads in Edinburgh
        val vpsAvailablePoint = Point(-3.1842032227822075, 55.94513813736027)
        // summit of Braeriach
        val vpsUnavailablePoint = Point(-3.728567847899427, 57.07812795418614)


        composeTestRule.setContent {
            val sessionWrapper = rememberArSessionWrapper(
                applicationContext = LocalContext.current.applicationContext,
                onError = { throw it },
                useGeospatial = false,
                planeFindingMode = Config.PlaneFindingMode.DISABLED
            )
            proxy.setSessionWrapper(sessionWrapper)
        }

        val vpsAvailableResult = proxy.checkVpsAvailability(vpsAvailablePoint.y, vpsAvailablePoint.x)
        val vpsUnavailableResult = proxy.checkVpsAvailability(vpsUnavailablePoint.y, vpsUnavailablePoint.x)

        assertThat(vpsAvailableResult.getOrNull()).isEqualTo(WorldScaleVpsAvailability.Available)
        assertThat(vpsUnavailableResult.getOrNull()).isEqualTo(WorldScaleVpsAvailability.Unavailable)
    }
}