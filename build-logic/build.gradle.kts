/*
 *
 *  Copyright 2026 Esri
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

plugins {
    `kotlin-dsl`
}

// Since the build-logic-internal build is not included for non-internal developers,
// plugins created there are not accessible to them. That means modules that
// apply plugins from build-logic-internal (e.g., `android-integration-testing`)
// would fail to compile.
//
// To avoid this, we register a placeholder plugin with the same ID in the
// publicly accessible build-logic build. This lets non-internal developers
// compile and work with the code, while internal developers get the real
// implementation from build-logic-internal.
gradlePlugin {
    plugins {
        create("androidIntegrationTesting") {
            id = "android-integration-testing"
            implementationClass = "EmptyPlugin"
        }
    }
}
