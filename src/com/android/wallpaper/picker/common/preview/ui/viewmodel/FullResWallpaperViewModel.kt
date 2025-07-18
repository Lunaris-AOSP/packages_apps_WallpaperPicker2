/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.wallpaper.picker.common.preview.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Point
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel

data class FullResWallpaperViewModel(
    val rawWallpaperBitmap: Bitmap,
    // TODO(b/348462236): remove this field and use rawWallpaperBitmap's width and height
    val rawWallpaperSize: Point,
    val asset: Asset,
    val fullPreviewCropModels: Map<Point, FullPreviewCropModel>?,
)
