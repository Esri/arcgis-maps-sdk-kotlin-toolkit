/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.ar

/**
 * Indicates an error with the authorization of ArCore on Google Cloud. This may be because:
 *
 * - The associated Google Cloud project may not have enabled the ARCore API.
 * - When using API key authentication, this will happen if the API key in the manifest is invalid or unauthorized. It may also fail if the API key is restricted to a set of apps not including the current one.
 * - When using keyless authentication, this may happen when no OAuth client has been created, or when the signing key and package name combination does not match the values used in the Google Cloud project. It may also fail if Google Play Services isn't installed, is too old, or is malfunctioning for some reason (e. g. killed due to memory pressure).
 *
 * For more information about ARCore authorization see [Use the ARCore API on Google Cloud](https://developers.google.com/ar/develop/authorization?platform=android).
 *
 * @since 200.7.0
 */
public class ArCoreAuthorizationException: Exception("The Google Cloud authorization provided by the application is not valid.")

/**
 * Indicates that the application has exhausted the quota allotted to the ARCore API on the given Google Cloud project. The developer should request additional quota for the ARCore API for their project from the Google Cloud Console.
 *
 * For more information about the ARCore request quota, see the Google documentation on [Quota and billing](https://developers.google.com/ar/develop/authorization?platform=android#quota-and-billing).
 *
 * @since 200.8.0
 */
public class ArCoreResourceExhaustedException: Exception("The application has exhausted the quota allotted to the given Google Cloud project. The developer should request additional quota for the ARCore API for their project from the Google Cloud Console.")
