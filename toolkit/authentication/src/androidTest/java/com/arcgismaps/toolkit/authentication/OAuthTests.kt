package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OAuthTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val arcGISOnline = "https://arcgis.com"
    private val portal = Portal(arcGISOnline, Portal.Connection.Authenticated)
    private val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            arcGISOnline,
            // This client ID is for demo purposes only. For use of the Authenticator in your own app,
            //            // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
            "aink3YEhnDNBBcJq",
            "kotlin-toolkit-authenticator-microapp://auth"
        )
    }

//    @Before
//    fun signOut() {
//        runBlocking {
//            ArcGISEnvironment.authenticationManager.signOut()
//        }
//    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
    @Test
    fun issueChallengeWithInput() = runTest {
        composeTestRule.setContent {
            var count by remember {
                mutableStateOf(0, neverEqualPolicy())
            }
            LaunchedEffect(Unit) {
                repeat(1000) {
                    delay(100)
                    count++
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Authenticator(authenticatorState = authenticatorState)
                Text(text = count.toString())
            }
        }

//        repeat(1000) {
//            delay(100)
//            composeTestRule.waitForIdle()
//        }
        val job = async(context = Dispatchers.IO) {
            portal.load()
        }
        job.await()
//        composeTestRule.waitUntilAtLeastOneExists(hasText("Sign in to ArcGIS Online"))

        assert(authenticatorState.pendingOAuthUserSignIn.value != null)
    }
}
//
//class OAuthTestActivity: ComponentActivity() {
//    private val viewModel by viewModels<OAuthTestActivityViewModel>()
//
//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//        setContent {
//            DialogAuthenticator(authenticatorState = viewModel.authenticatorState)
//        }
//    }
//}
//
//class OAuthTestActivityViewModel : ViewModel() {
//    val authenticatorState = AuthenticatorState().apply {
//        oAuthUserConfiguration = OAuthUserConfiguration(
//            "https://arcgis.com",
//            // This client ID is for demo purposes only. For use of the Authenticator in your own app,
//            // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
//            "aink3YEhnDNBBcJq",
//            "kotlin-toolkit-authenticator-microapp://auth"
//        )
//    }
//}