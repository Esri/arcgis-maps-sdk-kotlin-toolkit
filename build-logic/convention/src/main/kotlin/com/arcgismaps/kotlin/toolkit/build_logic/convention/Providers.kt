package com.arcgismaps.kotlin.toolkit.build_logic.convention

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.internal.cc.base.logger
import java.io.File

/**
 * A [ValueSource] that reads the contents of the `.vault-token` file in the user's home directory.
 * If the file does not exist or is empty, an empty string is returned.
 *
 * @since 300.2.0
 */
@Suppress("UNUSED")
abstract class VaultTokenProvider : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        var token: String? = null

        val tokenFile = File(System.getProperty("user.home"), ".vault-token")
        if (tokenFile.exists()) {
            tokenFile.inputStream().bufferedReader().use {
                token = it.readText()
            }
        }

        return if (token != null) {
            token
        } else {
            logger.warn("vault token does not exist or is empty, returning an empty string.")
            ""
        }
    }
}

