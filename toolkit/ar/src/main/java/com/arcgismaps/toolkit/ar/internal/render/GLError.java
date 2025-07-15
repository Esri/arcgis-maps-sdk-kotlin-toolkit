/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2024 Esri Inc
 */
package com.arcgismaps.toolkit.ar.internal.render;

import static com.arcgismaps.toolkit.ar.internal.render.DebugUtilKt.logArMessage;

import android.opengl.GLES30;
import android.opengl.GLException;
import android.opengl.GLU;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Module for handling OpenGL errors. This class is based on the [GLError](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/GLError.java)
 * from Google's Hello AR sample app.
 *
 * @since 200.6.0
 */
class GLError {
    /**
     * Throws a {@link GLException} if a GL error occurred.
     */
    public static void maybeThrowGLException(String reason, String api) {
        List<Integer> errorCodes = getGlErrors();
        if (errorCodes != null) {
            throw new GLException(errorCodes.get(0), formatErrorMessage(reason, api, errorCodes));
        }
    }

    /**
     * Logs a message with the given logcat priority if a GL error occurred.
     */
    public static void maybeLogGLError(String reason, String api) {
        List<Integer> errorCodes = getGlErrors();
        if (errorCodes != null) {
            logArMessage(formatErrorMessage(reason, api, errorCodes), null);
        }
    }

    private static String formatErrorMessage(String reason, String api, List<Integer> errorCodes) {
        StringBuilder builder = new StringBuilder(String.format("%s: %s: ", reason, api));
        Iterator<Integer> iterator = errorCodes.iterator();
        while (iterator.hasNext()) {
            int errorCode = iterator.next();
            builder.append(String.format("%s (%d)", GLU.gluErrorString(errorCode), errorCode));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private static List<Integer> getGlErrors() {
        int errorCode = GLES30.glGetError();
        // Shortcut for no errors
        if (errorCode == GLES30.GL_NO_ERROR) {
            return null;
        }
        List<Integer> errorCodes = new ArrayList<>();
        errorCodes.add(errorCode);
        while (true) {
            errorCode = GLES30.glGetError();
            if (errorCode == GLES30.GL_NO_ERROR) {
                break;
            }
            errorCodes.add(errorCode);
        }
        return errorCodes;
    }

    private GLError() {
    }
}
