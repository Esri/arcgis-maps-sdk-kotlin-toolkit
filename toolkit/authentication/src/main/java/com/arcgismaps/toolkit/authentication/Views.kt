package com.arcgismaps.toolkit.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

@Composable
public fun Authenticator(authenticatorViewModel: AuthenticatorViewModel = viewModel()) {
    ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler = authenticatorViewModel
    val oAuthPendingSignIn = authenticatorViewModel.pendingOAuthUserSignIn.collectAsState().value
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            authenticatorViewModel.onOAuthActivityResult(redirectUrl = activityResult.data?.toString())
        }
    }
    authenticatorViewModel.pendingOAuthUserSignIn.collectAsState().value?.let {
        OAuthCCT(launcher, oAuthPendingSignIn)
    }
//    val shouldShowDialog = authenticatorViewModel.shouldShowDialog.collectAsState().value
//    if (shouldShowDialog) {
//        AlertDialog(
//            onDismissRequest = authenticatorViewModel::dismissDialog,
//            confirmButton = {
//                Button(onClick = authenticatorViewModel::dismissDialog) {
//                    Text(text = "Confirm")
//                }
//            },
//            text = {
//                Text(text = "This is just a demo. " +
//                        "If you confirm or dismiss this dialog, it will not be displayed again.")
//            }
//        )
//    }
}

@Composable
private fun OAuthCCT(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>, oAuthPendingSignIn: OAuthUserSignIn?) {
    LaunchedEffect(oAuthPendingSignIn) {
        oAuthPendingSignIn?.let {
            launcher.launch(CustomTabsIntent.Builder().build().apply {
                intent.data = Uri.parse(oAuthPendingSignIn.authorizeUrl)
            }.intent)
        }
    }
}

private class OAuthContract : ActivityResultContract<OAuthUserSignIn, String?>() {
    lateinit var redirectUrl: String

    override fun createIntent(context: Context, input: OAuthUserSignIn) =
        run {
            redirectUrl = input.oAuthUserConfiguration.redirectUrl
            CustomTabsIntent.Builder().build().apply {
                intent.data = Uri.parse(input.authorizeUrl)
            }.intent
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        return if (resultCode == 1) {
            // the uri
            intent?.data?.let {
                val uriString = it.toString()
                if (uriString.startsWith(redirectUrl)) {
                    uriString
                } else null
            }
        } else {
            null
        }
    }
}