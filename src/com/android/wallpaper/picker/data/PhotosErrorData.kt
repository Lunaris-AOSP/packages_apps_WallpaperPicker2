/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.data

/**
 * This class contains possible error codes thrown by photos app and that can be handled differently
 * in the view layer.
 */
class PhotosErrorData(val message: String) {
    companion object {
        // These strings are not in strings.xml because in current implementation, we don't show
        // these on the screen directly, so they do not need translation.
        private const val NO_ERROR = "No error!"
        private const val UNIMPLEMENTED_MESSAGE = "The feature is not available!"
        private const val NO_PHOTOS_MESSAGE = "No suitable photos are available!"
        private const val UNAUTHENTICATED_MESSAGE = "Login to photos for suggested photos!"
        private const val DEFAULT_MESSAGE = "Unknown error!"

        val OK = PhotosErrorData(NO_ERROR)
        val UNIMPLEMENTED = PhotosErrorData(UNIMPLEMENTED_MESSAGE)
        val NO_PHOTOS = PhotosErrorData(NO_PHOTOS_MESSAGE)
        val UNAUTHENTICATED = PhotosErrorData(UNAUTHENTICATED_MESSAGE)
        val DEFAULT = PhotosErrorData(DEFAULT_MESSAGE)
    }
}
