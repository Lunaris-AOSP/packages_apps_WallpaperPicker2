/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wallpaper.picker.preview.shared.model

import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel

/**
 * Data class representing the status and the wallpaper from downloading a downloadable wallpaper.
 */
data class DownloadableWallpaperModel(
    val status: DownloadStatus,
    val wallpaperModel: LiveWallpaperModel?,
)

enum class DownloadStatus {
    DOWNLOAD_NOT_AVAILABLE,
    READY_TO_DOWNLOAD,
    DOWNLOADING,
    DOWNLOADED,
}
