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
package com.android.wallpaper

import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.module.DefaultRecentWallpaperManager
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.RecentWallpaperManager
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.TestUserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.modules.WallpaperPicker2AppModule
import com.android.wallpaper.network.Requester
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CuratedPhotosInteractor
import com.android.wallpaper.picker.category.domain.interactor.OnDeviceWallpapersInteractor
import com.android.wallpaper.picker.category.domain.interactor.ThirdPartyCategoryInteractor
import com.android.wallpaper.picker.category.ui.view.providers.IndividualPickerFactory
import com.android.wallpaper.picker.category.ui.view.providers.implementation.DefaultIndividualPickerFactory
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import com.android.wallpaper.picker.common.preview.ui.binder.DefaultWorkspaceCallbackBinder
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultToolbarBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.preview.ui.util.DefaultImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.testing.FakeCategoryInteractor
import com.android.wallpaper.testing.FakeCuratedPhotosInteractorImpl
import com.android.wallpaper.testing.FakeDefaultPhotosErrorConvertor
import com.android.wallpaper.testing.FakeDefaultRequester
import com.android.wallpaper.testing.FakeDefaultWallpaperCategoryClient
import com.android.wallpaper.testing.FakeDefaultWallpaperModelFactory
import com.android.wallpaper.testing.FakeOnDeviceWallpapersInteractor
import com.android.wallpaper.testing.FakeThirdPartyCategoryInteractor
import com.android.wallpaper.testing.FakeWallpaperCategoryWrapper
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestPartnerProvider
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.util.converter.PhotosErrorConvertor
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [WallpaperPicker2AppModule::class],
)
abstract class WallpaperPicker2TestModule {

    @Binds
    @Singleton
    abstract fun bindCustomizationOptionsBinder(
        impl: DefaultCustomizationOptionsBinder
    ): CustomizationOptionsBinder

    @Binds
    @Singleton
    abstract fun bindDefaultWallpaperCategoryClient(
        impl: FakeDefaultWallpaperCategoryClient
    ): DefaultWallpaperCategoryClient

    @Binds
    @Singleton
    abstract fun bindEffectsController(impl: FakeEffectsController): EffectsController

    @Binds
    @Singleton
    abstract fun bindIndividualPickerFactoryFragment(
        impl: DefaultIndividualPickerFactory
    ): IndividualPickerFactory

    @Binds
    @Singleton
    abstract fun bindCategoryInteractor(impl: FakeCategoryInteractor): CategoryInteractor

    @Binds
    @Singleton
    abstract fun bindCuratedPhotosInteractor(
        impl: FakeCuratedPhotosInteractorImpl
    ): CuratedPhotosInteractor

    @Binds
    @Singleton
    abstract fun bindDefaultPhotosErrorConvertor(
        impl: FakeDefaultPhotosErrorConvertor
    ): PhotosErrorConvertor

    @Binds
    @Singleton
    abstract fun bindImageEffectDialogUtil(
        impl: DefaultImageEffectDialogUtil
    ): ImageEffectDialogUtil

    @Binds @Singleton abstract fun bindInjector(impl: TestInjector): Injector

    @Binds
    @Singleton
    abstract fun bindOnDeviceWallpapersInteractor(
        impl: FakeOnDeviceWallpapersInteractor
    ): OnDeviceWallpapersInteractor

    @Binds @Singleton abstract fun bindPartnerProvider(impl: TestPartnerProvider): PartnerProvider

    @Binds @Singleton abstract fun bindRequester(impl: FakeDefaultRequester): Requester

    @Binds
    @Singleton
    abstract fun bindThirdPartyCategoryInteractor(
        impl: FakeThirdPartyCategoryInteractor
    ): ThirdPartyCategoryInteractor

    @Binds @Singleton abstract fun bindToolbarBinder(impl: DefaultToolbarBinder): ToolbarBinder

    @Binds @Singleton abstract fun bindUserEventLogger(impl: TestUserEventLogger): UserEventLogger

    @Binds
    @Singleton
    abstract fun bindWallpaperCategoryWrapper(
        impl: FakeWallpaperCategoryWrapper
    ): WallpaperCategoryWrapper

    @Binds
    @Singleton
    abstract fun bindWallpaperModelFactory(
        impl: FakeDefaultWallpaperModelFactory
    ): WallpaperModelFactory

    @Binds
    @Singleton
    abstract fun bindWallpaperPreferences(impl: TestWallpaperPreferences): WallpaperPreferences

    @Binds
    @Singleton
    abstract fun bindWorkspaceCallbackBinder(
        impl: DefaultWorkspaceCallbackBinder
    ): WorkspaceCallbackBinder

    @Binds
    @Singleton
    abstract fun bindRecentWallpaperManager(
        impl: DefaultRecentWallpaperManager
    ): RecentWallpaperManager
}
