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

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.filters.SmallTest
import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.picker.broadcast.BroadcastDispatcher
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.WallpaperModelUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@SmallTest
@RunWith(RobolectricTestRunner::class)
class WallpaperRepository2Test {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var client: FakeWallpaperClient
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var broadcastDispatcher: BroadcastDispatcher
    @Inject lateinit var underTest: WallpaperRepository2

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun currentWallpaperModels_updatesOnWallpaperChange() {
        testScope.runTest {
            val testWallpaperModels =
                WallpaperModelsPair(
                    WallpaperModelUtils.getStaticWallpaperModel(
                        wallpaperId = "testWallpaper",
                        collectionId = "testCollection",
                    ),
                    null,
                )
            client.setCurrentWallpaperModels(
                testWallpaperModels.homeWallpaper,
                testWallpaperModels.lockWallpaper,
            )
            var currentWallpaperModels: WallpaperModelsPair? = null
            val job = launch {
                underTest.currentWallpaperModels.collect { currentWallpaperModels = it }
            }

            // Enable the suspend function WallpaperClient#getCurrentWallpaperModels in the flow to
            // run
            runCurrent()

            // Make sure the initial wallpaper models are emitted to new subscribers
            assertThat(currentWallpaperModels).isEqualTo(testWallpaperModels)

            // Set new wallpaper
            val testWallpaperModels2 =
                WallpaperModelsPair(
                    WallpaperModelUtils.getStaticWallpaperModel(
                        wallpaperId = "testWallpaper2",
                        collectionId = "testCollection2",
                    ),
                    null,
                )
            client.setCurrentWallpaperModels(
                testWallpaperModels2.homeWallpaper,
                testWallpaperModels2.lockWallpaper,
            )

            // "Broadcast" wallpaper changed intent by finding the BroadcastReceiver associated with
            // the broadcast flow in WallpaperRepository2 and call onReceive
            val shadowApplication = Shadows.shadowOf(appContext as Application)
            val receivers = shadowApplication.registeredReceivers
            val capturedReceiver =
                receivers.find { it.intentFilter.matchAction(Intent.ACTION_WALLPAPER_CHANGED) }
            assertThat(capturedReceiver).isNotNull()
            capturedReceiver!!
                .broadcastReceiver
                .onReceive(appContext, Intent(Intent.ACTION_WALLPAPER_CHANGED))

            // Enable the suspend function WallpaperClient#getCurrentWallpaperModels in the flow to
            // run
            runCurrent()

            // Make sure updated wallpaper models are emitted to subscribers
            assertThat(currentWallpaperModels).isEqualTo(testWallpaperModels2)

            job.cancel()
        }
    }
}
