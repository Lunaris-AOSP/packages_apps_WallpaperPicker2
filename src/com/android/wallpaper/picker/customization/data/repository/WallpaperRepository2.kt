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
 *
 */

package com.android.wallpaper.picker.customization.data.repository

import android.content.Intent
import android.content.IntentFilter
import com.android.wallpaper.picker.broadcast.BroadcastDispatcher
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

// Based on WallpaperRepository, cleaned up with only functionality needed by new picker previews.
// TODO (b/367753950): clean up old code to eventually remove WallpaperRepository or combine with it
/** Encapsulates access to wallpaper-related data. */
@Singleton
class WallpaperRepository2
@Inject
constructor(
    @BackgroundDispatcher private val scope: CoroutineScope,
    private val client: WallpaperClient,
    broadcastDispatcher: BroadcastDispatcher,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) {
    private val wallpaperChanged =
        broadcastDispatcher.broadcastFlow(IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)).onStart {
            emit(Unit)
        }

    val currentWallpaperModels =
        wallpaperChanged
            .map { client.getCurrentWallpaperModels() }
            .flowOn(backgroundDispatcher)
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(), replay = 1)
}
