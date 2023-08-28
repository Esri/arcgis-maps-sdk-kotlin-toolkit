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

import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.TokenCredential


class FeatureFormsTestChallengeHandler(
    private val username: String,
    private val password: String
) : ArcGISAuthenticationChallengeHandler {
    override suspend fun handleArcGISAuthenticationChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val result: Result<TokenCredential> =
            TokenCredential.create(
                challenge.requestUrl,
                username,
                password,
                tokenExpirationInterval = 0
            )
        return result.let {
            if (it.isSuccess) {
                ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it.getOrThrow())
            } else {
                ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(it.exceptionOrNull()!!)
            }
        }
    }
}