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

package com.android.wallpaper.picker.di.modules

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import com.android.wallpaper.module.CreativeHelper
import com.android.wallpaper.module.DefaultCreativeHelper
import com.android.wallpaper.module.DefaultNetworkStatusNotifier
import com.android.wallpaper.module.DefaultPackageStatusNotifier
import com.android.wallpaper.module.DefaultWallpaperRefresher
import com.android.wallpaper.module.LargeScreenMultiPanesChecker
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.module.PackageStatusNotifier
import com.android.wallpaper.module.WallpaperRefresher
import com.android.wallpaper.network.Requester
import com.android.wallpaper.network.WallpaperRequester
import com.android.wallpaper.picker.MyPhotosStarter
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClientImpl
import com.android.wallpaper.picker.category.client.LiveWallpapersClient
import com.android.wallpaper.picker.category.client.LiveWallpapersClientImpl
import com.android.wallpaper.picker.category.data.repository.DefaultWallpaperCategoryRepository
import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.category.domain.interactor.MyPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.implementations.MyPhotosInteractorImpl
import com.android.wallpaper.picker.category.ui.view.MyPhotosStarterImpl
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.data.content.WallpaperClientImpl
import com.android.wallpaper.picker.network.data.DefaultNetworkStatusRepository
import com.android.wallpaper.picker.network.data.NetworkStatusRepository
import com.android.wallpaper.picker.network.domain.DefaultNetworkStatusInteractor
import com.android.wallpaper.picker.network.domain.NetworkStatusInteractor
import com.android.wallpaper.system.PowerManagerImpl
import com.android.wallpaper.system.PowerManagerWrapper
import com.android.wallpaper.system.UiModeManagerImpl
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.util.WallpaperParser
import com.android.wallpaper.util.WallpaperParserImpl
import com.android.wallpaper.util.converter.category.CategoryFactory
import com.android.wallpaper.util.converter.category.DefaultCategoryFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Qualifier for main thread [CoroutineDispatcher] bound to app lifecycle. */
@Qualifier annotation class MainDispatcher

/** Qualifier for background thread [CoroutineDispatcher] for long running and blocking tasks. */
@Qualifier annotation class BackgroundDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class SharedAppModule {

    @Binds
    @Singleton
    abstract fun bindCategoryFactory(impl: DefaultCategoryFactory): CategoryFactory

    @Binds @Singleton abstract fun bindCreativeHelper(impl: DefaultCreativeHelper): CreativeHelper

    @Binds
    @Singleton
    abstract fun bindLiveWallpapersClient(impl: LiveWallpapersClientImpl): LiveWallpapersClient

    @Binds
    @Singleton
    abstract fun bindMyPhotosInteractor(impl: MyPhotosInteractorImpl): MyPhotosInteractor

    @Binds
    @Singleton
    abstract fun bindNetworkStatusRepository(
        impl: DefaultNetworkStatusRepository
    ): NetworkStatusRepository

    @Binds
    @Singleton
    abstract fun bindNetworkStatusInteractor(
        impl: DefaultNetworkStatusInteractor
    ): NetworkStatusInteractor

    @Binds
    @Singleton
    abstract fun bindNetworkStatusNotifier(
        impl: DefaultNetworkStatusNotifier
    ): NetworkStatusNotifier

    @Binds @Singleton abstract fun bindRequester(impl: WallpaperRequester): Requester

    @Binds
    @Singleton
    abstract fun bindUiModeManagerWrapper(impl: UiModeManagerImpl): UiModeManagerWrapper

    @Binds
    @Singleton
    abstract fun bindPowerManagerWrapper(impl: PowerManagerImpl): PowerManagerWrapper

    @Binds
    @Singleton
    abstract fun bindWallpaperCategoryClient(
        impl: DefaultWallpaperCategoryClientImpl
    ): DefaultWallpaperCategoryClient

    @Binds
    @Singleton
    abstract fun bindPackageNotifier(impl: DefaultPackageStatusNotifier): PackageStatusNotifier

    @Binds
    @Singleton
    abstract fun bindWallpaperCategoryRepository(
        impl: DefaultWallpaperCategoryRepository
    ): WallpaperCategoryRepository

    @Binds @Singleton abstract fun bindWallpaperClient(impl: WallpaperClientImpl): WallpaperClient

    @Binds @Singleton abstract fun bindWallpaperParser(impl: WallpaperParserImpl): WallpaperParser

    @Binds
    @Singleton
    abstract fun bindWallpaperPickerDelegate2(impl: MyPhotosStarterImpl): MyPhotosStarter

    @Binds
    @Singleton
    abstract fun bindWallpaperRefresher(impl: DefaultWallpaperRefresher): WallpaperRefresher

    companion object {

        @Qualifier
        @MustBeDocumented
        @Retention(AnnotationRetention.RUNTIME)
        annotation class BroadcastRunning

        const val BROADCAST_SLOW_DISPATCH_THRESHOLD = 1000L
        const val BROADCAST_SLOW_DELIVERY_THRESHOLD = 1000L

        @Provides
        @BackgroundDispatcher
        fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @BackgroundDispatcher
        fun provideBackgroundScope(): CoroutineScope = CoroutineScope(Dispatchers.IO)

        /** Provide a BroadcastRunning Executor (for sending and receiving broadcasts). */
        @Provides
        @Singleton
        @BroadcastRunning
        fun provideBroadcastRunningExecutor(@BroadcastRunning looper: Looper?): Executor {
            val handler = Handler(looper ?: Looper.getMainLooper())
            return Executor { command -> handler.post(command) }
        }

        @Provides
        @Singleton
        @BroadcastRunning
        fun provideBroadcastRunningLooper(): Looper {
            return HandlerThread("BroadcastRunning", Process.THREAD_PRIORITY_BACKGROUND)
                .apply {
                    start()
                    looper.setSlowLogThresholdMs(
                        BROADCAST_SLOW_DISPATCH_THRESHOLD,
                        BROADCAST_SLOW_DELIVERY_THRESHOLD,
                    )
                }
                .looper
        }

        @Provides
        @MainDispatcher
        fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

        @Provides
        @MainDispatcher
        fun provideMainScope(): CoroutineScope = CoroutineScope(Dispatchers.Main)

        @Provides
        @Singleton
        fun provideMultiPanesChecker(): MultiPanesChecker {
            return LargeScreenMultiPanesChecker()
        }

        @Provides
        @Singleton
        fun providePackageManager(@ApplicationContext appContext: Context): PackageManager {
            return appContext.packageManager
        }

        @Provides
        @Singleton
        fun provideContentResolver(@ApplicationContext appContext: Context): ContentResolver {
            return appContext.contentResolver
        }

        @Provides
        @Singleton
        fun provideResources(@ApplicationContext context: Context): Resources {
            return context.resources
        }

        @Provides
        @Singleton
        fun provideWallpaperManager(@ApplicationContext appContext: Context): WallpaperManager {
            return WallpaperManager.getInstance(appContext)
        }
    }
}
