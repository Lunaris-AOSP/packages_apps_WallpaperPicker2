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

package com.android.wallpaper.picker.preview.shared.model

/** The sates of small preview pager motion layout. */
enum class SmallPreviewPagerStateModel {
    /* Showing lock preview page. */
    LOCK_SCREEN,
    /* Showing home preview page. */
    HOME_SCREEN,
    /* Transitioning to apply wallpaper screen. */
    TRANS_TO_APPLY_WALLPAPER_SCREEN,
    /* Showing apply wallpaper screen. */
    APPLY_WALLPAPER_SCREEN,
    /* Transitioning between two states excluding transition to apply wallpaper screen. */
    TRANS,
}

/** Returns true if the state is either home preview or lock preview. */
fun SmallPreviewPagerStateModel.isPagerState() =
    this == SmallPreviewPagerStateModel.LOCK_SCREEN ||
        this == SmallPreviewPagerStateModel.HOME_SCREEN

/** Returns true if in transition state. */
fun SmallPreviewPagerStateModel.isTransitionState() =
    this == SmallPreviewPagerStateModel.TRANS ||
        this == SmallPreviewPagerStateModel.TRANS_TO_APPLY_WALLPAPER_SCREEN

/** Returns true if the state is apply wallpaper screen. */
fun SmallPreviewPagerStateModel.isApplyWallpaperState() =
    this == SmallPreviewPagerStateModel.APPLY_WALLPAPER_SCREEN
