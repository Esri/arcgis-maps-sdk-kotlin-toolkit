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

import android.content.Intent
import android.net.Uri
import com.arcgismaps.httpcore.ArcGISHttpClient
import com.arcgismaps.httpcore.Request
import com.arcgismaps.httpcore.Response
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import io.mockk.every
import io.mockk.mockk

/**
 * A mock [ArcGISAuthenticationChallenge] for testing purposes.
 *
 * @since 200.5.0
 */
fun makeMockArcGISAuthenticationChallenge() = mockk<ArcGISAuthenticationChallenge>().apply {
    every { requestUrl } returns "https://arcgis.com"
    every { cause } returns Throwable()
}

/**
 * Returns an intent that simulates a successful redirect from the OAuth server.
 *
 * @since 200.5.0
 */
fun getSuccessfulRedirectIntent(redirectUrl: String): Intent {
    return Intent().apply {
        data = Uri.parse("$redirectUrl?code=12345")
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

/**
 * Intercepts requests to validate a token and returns a fake response that validates the token.
 *
 * @since 200.5.0
 */
fun ArcGISHttpClient.Builder.interceptTokenRequests() {
    interceptor { chain ->
        chain.request().let { request ->
            if (request.url.endsWith("sharing/rest/oauth2/token")) {
                request.getFakeTokenResponse()
            } else chain.proceed(request)
        }
    }
}

/**
 * Returns a fake response that validates the token.
 *
 * @since 200.5.0
 */
fun Request.getFakeTokenResponse(): Response =
    Response.builder().apply {
        request(this@getFakeTokenResponse)
        val bodyString =
            """
            {"access_token":"12345","expires_in":1800,"username":"username","ssl":true,"refresh_token":"67890","refresh_token_expires_in":1209599}
            """.trimIndent()
        body(
            Response.Body.builder().contentType("text/plain")
                .data(bodyString.byteInputStream(), bodyString.length.toLong()).build()
        )
        addHeader("cache-control", "no-cache, no-store, must-revalidate")
        addHeader("connection", "keep-alive")
        addHeader("content-encoding", "gzip")
        addHeader("content-type", "text/plain;charset=utf-8")
        addHeader("date", "Wed, 22 May 2024 15:04:50 GMT")
        addHeader("expires", "0")
        addHeader("pragma", "no-cache")
        addHeader("response-status-code", "200")
        addHeader("strict-transport-security", "max-age=31536000")
        addHeader("transfer-encoding", "chunked")
        addHeader("vary", "X-Esri-Authorization")
        addHeader("x-content-type-options", "nosniff")
    }.build()
