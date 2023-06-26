// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    @Suppress("DSL_SCOPE_VIOLATION") // fixed in gradle 8.1
    alias(libs.plugins.android.application) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.android.library) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.android) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.gradle.secrets) apply false
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.serialization) apply false
}
