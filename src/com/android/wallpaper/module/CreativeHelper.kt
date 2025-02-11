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

package com.android.wallpaper.module

import android.app.WallpaperInfo
import android.app.wallpaper.WallpaperDescription
import android.content.Context
import android.net.Uri
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination

/**
 * Interface for getting information from creative wallpapers needed to transition to content
 * handling or maintain compatibility with pre-content handling versions.
 */
interface CreativeHelper {

    /** Queries a live wallpaper for its preview Uri, and returns it if it exists. */
    fun getCreativePreviewUri(
        context: Context,
        info: WallpaperInfo,
        destination: WallpaperDestination,
    ): Uri?

    /** Queries a live wallpaper for its description, and returns it if it exists. */
    fun getCreativeDescription(
        context: Context,
        info: WallpaperInfo,
        destination: WallpaperDestination,
    ): WallpaperDescription?
}
