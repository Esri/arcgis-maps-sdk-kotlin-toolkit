/*
 * Copyright 2026 Esri
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

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.cacheFile
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.deleteIfExists
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import com.google.common.truth.Truth.assertThat

/**
 * Test class for various file operations and their internal utilities related to attachments in the
 * Feature Forms toolkit.
 *
 * @since 300.1.0
 */
class AttachmentFileOperationTests {

    /**
     * The context of the application under test.
     */
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Given a source file with content,
     * When the file is cached using the [cacheFile] extension function,
     * Then the cached file is created successfully and has the expected properties and content.
     *
     * @since 300.1.0
     */
    @Test
    fun testCachedFileCreation() = runTest {
        val source = AttachmentsFileProvider.createTempFileWithUri(
            prefix = "source",
            suffix = ".txt",
            context = context
        )
        source.file.writeText("Attachment content")

        var cachedFile : File? = null
        try {
            cachedFile = context.cacheFile(
                uri = source.uri,
                extension = "txt"
            ).getOrThrow()
            assertThat(cachedFile.exists()).isTrue()
            assertThat(cachedFile.readText()).isEqualTo("Attachment content")
            assertThat(source.file.absolutePath).isNotEqualTo(cachedFile.absolutePath)
            assertThat(cachedFile.name).endsWith(".txt")
        } finally {
            // Clean up the temporary files
            source.file.deleteIfExists()
            cachedFile?.deleteIfExists()
        }

        assertThat(source.file.exists()).isFalse()
        assertThat(cachedFile.exists()).isFalse()
    }

    /**
     * Given a non-existing source,
     * When the [cacheFile] extension function is called with the non-existing source,
     * Then the function returns a failure result and leaves no temp files.
     *
     * @since 300.1.0
     */
    @Test
    fun testCachedFileCreationWithNonExistingSource() = runTest {
        // Track the files in the cache directory before the operation
        val filesBefore = context.cacheDir
            .listFiles()
            .orEmpty()
            .map(File::getAbsolutePath)
            .toSet()
        val invalidUri = Uri.parse(
            "content://${context.packageName}.invalid/missing.txt"
        )
        val result = context.cacheFile(
            uri = invalidUri,
            extension = "txt"
        )
        // Track the files in the cache directory after the operation
        val filesAfter = context.cacheDir
            .listFiles()
            .orEmpty()
            .map(File::getAbsolutePath)
            .toSet()
        assertThat(result.isFailure).isTrue()
        // No new files should be created
        assertThat(filesAfter).isEqualTo(filesBefore)
    }
}
