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
package com.android.wallpaper.picker.preview.ui.viewmodel

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.util.DisplaysProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine

/** View model for static wallpaper preview used in [WallpaperPreviewActivity] and its fragments */
@ViewModelScoped
class StaticWallpaperPreviewViewModel
@Inject
constructor(
    interactor: WallpaperPreviewInteractor,
    @ApplicationContext private val context: Context,
    private val wallpaperPreferences: WallpaperPreferences,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
    viewModelScope: CoroutineScope,
    displaysProvider: DisplaysProvider,
) {
    /**
     * The state of static wallpaper crop in full preview, before user confirmation.
     *
     * The initial value should be the default crop on small preview, which could be the cropHints
     * for current wallpaper or default crop area for a new wallpaper.
     */
    val fullPreviewCropModels: MutableMap<Point, FullPreviewCropModel> = mutableMapOf()

    /**
     * The default crops for the current wallpaper, which is center aligned on the preview.
     *
     * Always update default through [updateDefaultPreviewCropModel] to make sure multiple updates
     * of the same preview only counts the first time it appears.
     */
    private val defaultPreviewCropModels: MutableMap<Point, FullPreviewCropModel> = mutableMapOf()

    /**
     * The info picker needs to post process crops for setting static wallpaper.
     *
     * It will be filled with current cropHints when previewing current wallpaper, and null when
     * previewing a new wallpaper, and gets updated through [updateCropHintsInfo] when user picks a
     * new crop.
     */
    @get:VisibleForTesting
    val cropHintsInfo: MutableStateFlow<Map<Point, FullPreviewCropModel>?> = MutableStateFlow(null)

    private val cropHints: Flow<Map<Point, Rect>?> =
        cropHintsInfo.map { cropHintsInfoMap ->
            cropHintsInfoMap?.map { entry -> entry.key to entry.value.cropHint }?.toMap()
        }

    val staticWallpaperModel: Flow<StaticWallpaperModel> =
        interactor.wallpaperModel.map { it as? StaticWallpaperModel }.filterNotNull()

    /** Null indicates the wallpaper has no low res image. */
    val lowResBitmap: Flow<Bitmap?> =
        staticWallpaperModel
            .map { it.staticWallpaperData.asset.getLowResBitmap(context) }
            .flowOn(bgDispatcher)
    // Asset detail includes the dimensions, bitmap and the asset.
    private val assetDetail: Flow<Triple<Point, Bitmap?, Asset>?> =
        interactor.wallpaperModel
            .map { (it as? StaticWallpaperModel)?.staticWallpaperData?.asset }
            .map { asset ->
                asset?.decodeRawDimensions()?.let { Triple(it, asset.decodeBitmap(it), asset) }
            }
            .flowOn(bgDispatcher)
            // We only want to decode bitmap every time when wallpaper model is updated, instead of
            // a new subscriber listens to this flow. So we need to use shareIn.
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    var scaleAndCenter: FullResImageViewUtil.ScaleAndCenter? = null

    val fullResWallpaperViewModel: Flow<FullResWallpaperViewModel?> =
        combine(assetDetail, cropHintsInfo) { assetDetail, cropHintsInfo ->
                if (assetDetail == null) {
                    null
                } else {
                    val (dimensions, bitmap, asset) = assetDetail
                    bitmap?.let {
                        FullResWallpaperViewModel(bitmap, dimensions, asset, cropHintsInfo)
                    }
                }
            }
            .flowOn(bgDispatcher)
    val subsamplingScaleImageViewModel: Flow<FullResWallpaperViewModel> =
        fullResWallpaperViewModel.filterNotNull()

    // At least as many crops as how many displays, it could be more due to the orientation. Or when
    // no crops ever set, unblocks down stream for default behavior.
    private val hasAllDisplayCrops: Flow<Boolean> =
        cropHintsInfo.map { it == null || it.size >= displaysProvider.getInternalDisplays().size }

    // TODO (b/315856338): cache wallpaper colors in preferences
    private val storedWallpaperColors: Flow<WallpaperColors?> =
        staticWallpaperModel
            .map { wallpaperPreferences.getWallpaperColors(it.commonWallpaperData.id.uniqueId) }
            .distinctUntilChanged()
    val wallpaperColors: Flow<WallpaperColorsModel> =
        combine(
                storedWallpaperColors,
                subsamplingScaleImageViewModel,
                cropHints,
                hasAllDisplayCrops.filter { it },
            ) { storedColors, wallpaperViewModel, cropHints, _ ->
                WallpaperColorsModel.Loaded(
                    if (cropHints == null) {
                        storedColors
                            ?: interactor.getWallpaperColors(
                                wallpaperViewModel.rawWallpaperBitmap,
                                null,
                            )
                    } else {
                        interactor.getWallpaperColors(
                            wallpaperViewModel.rawWallpaperBitmap,
                            cropHints,
                        )
                    }
                )
            }
            .distinctUntilChanged()

    /**
     * Updates new cropHints per displaySize that's been confirmed by the user or from a new default
     * crop.
     *
     * That's when picker gets current cropHints from [WallpaperManager] or when user crops and
     * confirms a crop, or when a small preview for a new display size has been discovered the first
     * time.
     */
    fun updateCropHintsInfo(
        cropHintsInfo: Map<Point, FullPreviewCropModel>,
        updateDefaultCrop: Boolean = false,
    ) {
        val newInfo =
            this.cropHintsInfo.value?.let { currentCropHintsInfo ->
                currentCropHintsInfo.plus(
                    if (updateDefaultCrop)
                        cropHintsInfo.filterKeys { !currentCropHintsInfo.keys.contains(it) }
                    else cropHintsInfo
                )
            } ?: cropHintsInfo
        this.cropHintsInfo.value = newInfo
        fullPreviewCropModels.putAll(newInfo)
    }

    /** Updates default cropHint for [displaySize] if it's not already exist. */
    fun updateDefaultPreviewCropModel(displaySize: Point, cropModel: FullPreviewCropModel) {
        defaultPreviewCropModels.let { cropModels ->
            if (!cropModels.contains(displaySize)) {
                cropModels[displaySize] = cropModel
                updateCropHintsInfo(
                    cropModels.filterKeys { it == displaySize },
                    updateDefaultCrop = true,
                )
            }
        }
    }

    // TODO b/296288298 Create a util class for Bitmap and Asset
    private suspend fun Asset.decodeRawDimensions(): Point? =
        suspendCancellableCoroutine { k: CancellableContinuation<Point?> ->
            val callback = Asset.DimensionsReceiver { k.resumeWith(Result.success(it)) }
            decodeRawDimensions(null, callback)
        }

    // TODO b/296288298 Create a util class functions for Bitmap and Asset
    private suspend fun Asset.decodeBitmap(dimensions: Point): Bitmap? =
        suspendCancellableCoroutine { k: CancellableContinuation<Bitmap?> ->
            val callback = Asset.BitmapReceiver { k.resumeWith(Result.success(it)) }
            decodeBitmap(dimensions.x, dimensions.y, /* hardwareBitmapAllowed= */ false, callback)
        }

    class Factory
    @Inject
    constructor(
        private val interactor: WallpaperPreviewInteractor,
        @ApplicationContext private val context: Context,
        private val wallpaperPreferences: WallpaperPreferences,
        @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
        private val displaysProvider: DisplaysProvider,
    ) {
        fun create(viewModelScope: CoroutineScope): StaticWallpaperPreviewViewModel {
            return StaticWallpaperPreviewViewModel(
                interactor = interactor,
                context = context,
                wallpaperPreferences = wallpaperPreferences,
                bgDispatcher = bgDispatcher,
                viewModelScope = viewModelScope,
                displaysProvider = displaysProvider,
            )
        }
    }
}
