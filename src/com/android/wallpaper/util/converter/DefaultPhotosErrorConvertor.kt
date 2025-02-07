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

package com.android.wallpaper.util.converter

import com.android.wallpaper.picker.data.PhotosErrorData
import io.grpc.Status
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is the default implementation for converting errors return from the photos app to something
 * that is understandable by the view layer.
 */
@Singleton
class DefaultPhotosErrorConvertor @Inject constructor() : PhotosErrorConvertor {
    override fun handleError(status: Status): PhotosErrorData {
        TODO("Not yet implemented")
    }
}
