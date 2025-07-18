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
 *
 */

package com.android.wallpaper.picker.customization.data.content

import android.app.Flags.liveWallpaperContentHandling
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.app.WallpaperManager.SetWallpaperFlags
import android.app.wallpaper.WallpaperDescription
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.android.app.tracing.TraceUtils.traceAsync
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.asset.BitmapUtils
import com.android.wallpaper.asset.CurrentWallpaperAsset
import com.android.wallpaper.asset.StreamableAsset
import com.android.wallpaper.model.LiveWallpaperPrefMetadata
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.StaticWallpaperPrefMetadata
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.WallpaperModelsPair
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.module.RecentWallpaperManager
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.BOTH
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.Companion.toDestinationInt
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.Companion.toSetWallpaperFlags
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.HOME
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination.LOCK
import com.android.wallpaper.picker.customization.shared.model.WallpaperModel as RecentWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.util.CurrentWallpaperInfoUtils.getCurrentWallpapers
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class WallpaperClientImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperManager: WallpaperManager,
    private val wallpaperPreferences: WallpaperPreferences,
    private val wallpaperModelFactory: WallpaperModelFactory,
    private val logger: UserEventLogger,
    private val recentWallpaperManager: RecentWallpaperManager,
    @BackgroundDispatcher val backgroundScope: CoroutineScope,
) : WallpaperClient {

    private var recentsContentProviderAvailable: Boolean? = null
    private val recentHomeWallpapers = MutableStateFlow<List<RecentWallpaperModel>?>(null)
    private val recentLockWallpapers = MutableStateFlow<List<RecentWallpaperModel>?>(null)

    init {
        backgroundScope.launch {
            recentHomeWallpapers.value = queryRecentWallpapers(destination = HOME)
            recentLockWallpapers.value = queryRecentWallpapers(destination = LOCK)
        }

        if (areRecentsAvailable()) {
            context.contentResolver.registerContentObserver(
                LIST_RECENTS_URI,
                /* notifyForDescendants= */ true,
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        backgroundScope.launch {
                            recentHomeWallpapers.value = queryRecentWallpapers(destination = HOME)
                            recentLockWallpapers.value = queryRecentWallpapers(destination = LOCK)
                        }
                    }
                },
            )
        }
    }

    override fun recentWallpapers(destination: WallpaperDestination, limit: Int) =
        when (destination) {
            HOME ->
                recentHomeWallpapers.asStateFlow().filterNotNull().map { wallpapers ->
                    if (wallpapers.size > limit) {
                        wallpapers.subList(0, limit)
                    } else {
                        wallpapers
                    }
                }
            LOCK ->
                recentLockWallpapers.asStateFlow().filterNotNull().map { wallpapers ->
                    if (wallpapers.size > limit) {
                        wallpapers.subList(0, limit)
                    } else {
                        wallpapers
                    }
                }
            BOTH ->
                throw IllegalStateException(
                    "Destination $destination should not be used for getting recent wallpapers."
                )
        }

    override suspend fun setStaticWallpaper(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: StaticWallpaperModel,
        bitmap: Bitmap,
        wallpaperSize: Point,
        asset: Asset,
        fullPreviewCropModels: Map<Point, FullPreviewCropModel>?,
    ) {
        if (destination == HOME || destination == BOTH) {
            // Disable rotation wallpaper when setting to home screen. Daily rotation rotates
            // both home and lock screen wallpaper when lock screen is not set; otherwise daily
            // rotation only rotates home screen while lock screen wallpaper stays as what it's
            // set to.
            stopWallpaperRotation()
        }

        traceAsync(TAG, "setStaticWallpaper") {
            val cropHintsWithParallax =
                fullPreviewCropModels?.let { cropModels ->
                    cropModels.mapValues { it.value.adjustCropForParallax(wallpaperSize) }
                } ?: emptyMap()
            val managerId =
                wallpaperManager.setStaticWallpaperToSystem(
                    asset.getStreamOrFromBitmap(bitmap),
                    bitmap,
                    cropHintsWithParallax,
                    destination,
                    asset,
                )

            wallpaperPreferences.setStaticWallpaperMetadata(
                metadata = wallpaperModel.getMetadata(bitmap, managerId),
                destination = destination,
            )

            logger.logWallpaperApplied(
                collectionId = wallpaperModel.commonWallpaperData.id.collectionId,
                wallpaperId = wallpaperModel.commonWallpaperData.id.wallpaperId,
                effects = null,
                setWallpaperEntryPoint = setWallpaperEntryPoint,
                destination =
                    UserEventLogger.toWallpaperDestinationForLogging(destination.toDestinationInt()),
            )

            // Save the static wallpaper to recent wallpapers
            // TODO(b/309138446): check if we can update recent with all cropHints from WM later
            wallpaperPreferences.addStaticWallpaperToRecentWallpapers(
                destination,
                wallpaperModel,
                bitmap,
                cropHintsWithParallax,
            )
        }
    }

    private fun stopWallpaperRotation() {
        wallpaperPreferences.setWallpaperPresentationMode(
            WallpaperPreferences.PRESENTATION_MODE_STATIC
        )
        wallpaperPreferences.clearDailyRotations()
    }

    /**
     * Use [WallpaperManager] to set a static wallpaper to the system.
     *
     * @return Wallpaper manager ID
     */
    private fun WallpaperManager.setStaticWallpaperToSystem(
        inputStream: InputStream?,
        bitmap: Bitmap,
        cropHints: Map<Point, Rect>,
        destination: WallpaperDestination,
        asset: Asset,
    ): Int {
        // The InputStream of current wallpaper points to system wallpaper file which will be
        // overwritten during set wallpaper and reads 0 bytes, use Bitmap instead.
        return if (inputStream != null && asset !is CurrentWallpaperAsset) {
            setStreamWithCrops(
                inputStream,
                cropHints,
                /* allowBackup= */ true,
                destination.toSetWallpaperFlags(),
            )
        } else {
            setBitmapWithCrops(
                bitmap,
                cropHints,
                /* allowBackup= */ true,
                destination.toSetWallpaperFlags(),
            )
        }
    }

    private fun StaticWallpaperModel.getMetadata(
        bitmap: Bitmap,
        managerId: Int,
    ): StaticWallpaperPrefMetadata {
        val bitmapHash = BitmapUtils.generateHashCode(bitmap)
        return StaticWallpaperPrefMetadata(
            commonWallpaperData.attributions,
            commonWallpaperData.exploreActionUrl,
            commonWallpaperData.id.collectionId,
            bitmapHash,
            managerId,
            commonWallpaperData.id.uniqueId,
            imageWallpaperData?.uri,
        )
    }

    /**
     * Save wallpaper metadata in the preference for two purposes:
     * 1. Quickly reconstruct the currently-selected wallpaper when opening the app
     * 2. Snapshot logging
     */
    private fun WallpaperPreferences.setStaticWallpaperMetadata(
        metadata: StaticWallpaperPrefMetadata,
        destination: WallpaperDestination,
    ) {
        when (destination) {
            HOME -> {
                clearHomeWallpaperMetadata()
                setHomeStaticImageWallpaperMetadata(metadata)
            }
            LOCK -> {
                clearLockWallpaperMetadata()
                setLockStaticImageWallpaperMetadata(metadata)
            }
            BOTH -> {
                clearHomeWallpaperMetadata()
                setHomeStaticImageWallpaperMetadata(metadata)
                clearLockWallpaperMetadata()
                setLockStaticImageWallpaperMetadata(metadata)
            }
        }
    }

    override suspend fun setLiveWallpaper(
        setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: LiveWallpaperModel,
    ) {
        if (destination == HOME || destination == BOTH) {
            // Disable rotation wallpaper when setting to home screen. Daily rotation rotates
            // both home and lock screen wallpaper when lock screen is not set; otherwise daily
            // rotation only rotates home screen while lock screen wallpaper stays as what it's
            // set to.
            stopWallpaperRotation()
        }

        traceAsync(TAG, "setLiveWallpaper") {
            val managerId = wallpaperManager.setLiveWallpaperToSystem(wallpaperModel, destination)

            wallpaperPreferences.setLiveWallpaperMetadata(
                metadata = wallpaperModel.getMetadata(managerId),
                destination = destination,
            )

            logger.logWallpaperApplied(
                collectionId = wallpaperModel.commonWallpaperData.id.collectionId,
                wallpaperId = wallpaperModel.commonWallpaperData.id.wallpaperId,
                effects = wallpaperModel.liveWallpaperData.effectNames,
                setWallpaperEntryPoint = setWallpaperEntryPoint,
                destination =
                    UserEventLogger.toWallpaperDestinationForLogging(destination.toDestinationInt()),
            )

            wallpaperPreferences.addLiveWallpaperToRecentWallpapers(destination, wallpaperModel)
        }
    }

    private fun tryAndroidBSetComponent(
        wallpaperModel: LiveWallpaperModel,
        destination: WallpaperDestination,
    ): Boolean {
        try {
            val method =
                wallpaperManager.javaClass.getMethod(
                    "setWallpaperComponentWithDescription",
                    WallpaperDescription::class.java,
                    Int::class.javaPrimitiveType,
                )
            method.invoke(
                wallpaperManager,
                wallpaperModel.liveWallpaperData.description,
                destination.toSetWallpaperFlags(),
            )
            return true
        } catch (e: NoSuchMethodException) {
            return false
        }
    }

    private fun tryAndroidUSetComponent(
        wallpaperModel: LiveWallpaperModel,
        destination: WallpaperDestination,
    ): Boolean {
        try {
            val method =
                wallpaperManager.javaClass.getMethod(
                    "setWallpaperComponentWithFlags",
                    ComponentName::class.java,
                    Int::class.javaPrimitiveType,
                )
            method.invoke(
                wallpaperManager,
                wallpaperModel.commonWallpaperData.id.componentName,
                destination.toSetWallpaperFlags(),
            )
            if (liveWallpaperContentHandling()) {
                Log.w(
                    TAG,
                    "live wallpaper content handling enabled, but Android U setWallpaperComponentWithFlags called",
                )
            }
            return true
        } catch (e: NoSuchMethodException) {
            return false
        }
    }

    /**
     * Use [WallpaperManager] to set a live wallpaper to the system.
     *
     * @return Wallpaper manager ID
     */
    private fun WallpaperManager.setLiveWallpaperToSystem(
        wallpaperModel: LiveWallpaperModel,
        destination: WallpaperDestination,
    ): Int {
        if (tryAndroidBSetComponent(wallpaperModel, destination)) {
            // intentional no-op
        } else if (tryAndroidUSetComponent(wallpaperModel, destination)) {
            // intentional no-op
        } else {
            setWallpaperComponent(wallpaperModel.commonWallpaperData.id.componentName)
        }

        // Be careful that WallpaperManager.getWallpaperId can only accept either
        // WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK.
        // If destination is BOTH, either flag should return the same wallpaper manager ID.
        return getWallpaperId(
            if (destination == BOTH || destination == HOME) FLAG_SYSTEM else FLAG_LOCK
        )
    }

    private fun LiveWallpaperModel.getMetadata(managerId: Int): LiveWallpaperPrefMetadata {
        return LiveWallpaperPrefMetadata(
            commonWallpaperData.attributions,
            liveWallpaperData.systemWallpaperInfo.serviceName,
            liveWallpaperData.effectNames,
            commonWallpaperData.id.collectionId,
            managerId,
        )
    }

    /**
     * Save wallpaper metadata in the preference for two purposes:
     * 1. Quickly reconstruct the currently-selected wallpaper when opening the app
     * 2. Snapshot logging
     */
    private fun WallpaperPreferences.setLiveWallpaperMetadata(
        metadata: LiveWallpaperPrefMetadata,
        destination: WallpaperDestination,
    ) {
        when (destination) {
            HOME -> {
                clearHomeWallpaperMetadata()
                setHomeLiveWallpaperMetadata(metadata)
            }
            LOCK -> {
                clearLockWallpaperMetadata()
                setLockLiveWallpaperMetadata(metadata)
            }
            BOTH -> {
                clearHomeWallpaperMetadata()
                setHomeLiveWallpaperMetadata(metadata)
                clearLockWallpaperMetadata()
                setLockLiveWallpaperMetadata(metadata)
            }
        }
    }

    override suspend fun setRecentWallpaper(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperId: String,
        onDone: () -> Unit,
    ) {
        val updateValues = ContentValues()
        updateValues.put(KEY_ID, wallpaperId)
        updateValues.put(KEY_SCREEN, destination.asString())
        updateValues.put(KEY_SET_WALLPAPER_ENTRY_POINT, setWallpaperEntryPoint)
        traceAsync(TAG, "setRecentWallpaper") {
            val updatedRowCount =
                context.contentResolver.update(SET_WALLPAPER_URI, updateValues, null)
            if (updatedRowCount == 0) {
                Log.e(TAG, "Error setting wallpaper: $wallpaperId")
            }
            onDone.invoke()
        }
    }

    private suspend fun queryRecentWallpapers(
        destination: WallpaperDestination
    ): List<RecentWallpaperModel> =
        if (!areRecentsAvailable()) {
            listOf(getCurrentWallpaperFromFactory(destination))
        } else {
            queryAllRecentWallpapers(destination)
        }

    private fun queryAllRecentWallpapers(
        destination: WallpaperDestination
    ): List<RecentWallpaperModel> {
        context.contentResolver
            .query(
                LIST_RECENTS_URI.buildUpon().appendPath(destination.asString()).build(),
                arrayOf(KEY_ID, KEY_PLACEHOLDER_COLOR, KEY_LAST_UPDATED),
                null,
                null,
            )
            .use { cursor ->
                if (cursor == null || cursor.count == 0) {
                    return emptyList()
                }

                return buildList {
                    val idColumnIndex = cursor.getColumnIndex(KEY_ID)
                    val placeholderColorColumnIndex = cursor.getColumnIndex(KEY_PLACEHOLDER_COLOR)
                    val lastUpdatedColumnIndex = cursor.getColumnIndex(KEY_LAST_UPDATED)
                    val titleColumnIndex = cursor.getColumnIndex(TITLE)
                    while (cursor.moveToNext()) {
                        val wallpaperId = cursor.getString(idColumnIndex)
                        val placeholderColor = cursor.getInt(placeholderColorColumnIndex)
                        val lastUpdated = cursor.getLong(lastUpdatedColumnIndex)
                        val title =
                            if (titleColumnIndex > -1) cursor.getString(titleColumnIndex) else null

                        add(
                            RecentWallpaperModel(
                                wallpaperId = wallpaperId,
                                placeholderColor = placeholderColor,
                                lastUpdated = lastUpdated,
                                title = title,
                            )
                        )
                    }
                }
            }
    }

    private suspend fun getCurrentWallpaperFromFactory(
        destination: WallpaperDestination
    ): RecentWallpaperModel {
        val currentWallpapers =
            getCurrentWallpapers(context, updateRecents = false, forceRefresh = false) {
                info,
                screen ->
                recentWallpaperManager.getCurrentWallpaperBitmapUri(info, screen)
            }
        val wallpaper: WallpaperInfo =
            if (destination == LOCK) {
                currentWallpapers.second
            } else {
                currentWallpapers.first
            }
        val colors = wallpaperManager.getWallpaperColors(destination.toSetWallpaperFlags())

        return RecentWallpaperModel(
            wallpaperId = wallpaper.wallpaperId,
            placeholderColor = colors?.primaryColor?.toArgb() ?: Color.TRANSPARENT,
            title = wallpaper.getTitle(context),
        )
    }

    override suspend fun getCurrentWallpaperModels(forceRefresh: Boolean): WallpaperModelsPair {
        val currentWallpapers =
            getCurrentWallpapers(context, updateRecents = false, forceRefresh) { info, screen ->
                recentWallpaperManager.getCurrentWallpaperBitmapUri(info, screen)
            }
        val homeWallpaper = currentWallpapers.first
        val lockWallpaper = currentWallpapers.second
        return WallpaperModelsPair(
            wallpaperModelFactory.getWallpaperModel(context, homeWallpaper),
            wallpaperModelFactory.getWallpaperModel(context, lockWallpaper),
        )
    }

    override suspend fun loadThumbnail(
        wallpaperId: String,
        destination: WallpaperDestination,
    ): Bitmap? {
        if (areRecentsAvailable()) {
            try {
                // We're already using this in a suspend function, so we're okay.
                @Suppress("BlockingMethodInNonBlockingContext")
                context.contentResolver
                    .openFile(
                        GET_THUMBNAIL_BASE_URI.buildUpon()
                            .appendPath(wallpaperId)
                            .appendQueryParameter(KEY_DESTINATION, destination.asString())
                            .build(),
                        "r",
                        null,
                    )
                    .use { file ->
                        if (file == null) {
                            Log.e(TAG, "Error getting wallpaper preview: $wallpaperId")
                        } else {
                            return BitmapFactory.decodeFileDescriptor(file.fileDescriptor)
                        }
                    }
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Error getting wallpaper preview: $wallpaperId, destination: ${destination.asString()}",
                    e,
                )
            }
        } else {
            val currentWallpapers =
                getCurrentWallpapers(context, updateRecents = false, forceRefresh = false) {
                    info,
                    screen ->
                    recentWallpaperManager.getCurrentWallpaperBitmapUri(info, screen)
                }
            val wallpaper =
                if (currentWallpapers.first.wallpaperId == wallpaperId) {
                    currentWallpapers.first
                } else if (currentWallpapers.second.wallpaperId == wallpaperId) {
                    currentWallpapers.second
                } else null
            return wallpaper?.getThumbAsset(context)?.getLowResBitmap(context)
        }

        return null
    }

    override fun areRecentsAvailable(): Boolean {
        if (recentsContentProviderAvailable == null) {
            recentsContentProviderAvailable =
                try {
                    context.packageManager.resolveContentProvider(AUTHORITY, 0) != null
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Exception trying to resolve recents content provider, skipping it",
                        e,
                    )
                    false
                }
        }
        return recentsContentProviderAvailable == true
    }

    override fun getCurrentCropHints(
        displaySizes: List<Point>,
        @SetWallpaperFlags which: Int,
    ): Map<Point, Rect>? {
        val flags = InjectorProvider.getInjector().getFlags()
        if (!flags.isMultiCropEnabled()) {
            return null
        }
        val cropHints: List<Rect>? =
            wallpaperManager.getBitmapCrops(displaySizes, which, /* originalBitmap= */ true)

        return cropHints?.indices?.associate { displaySizes[it] to cropHints[it] }
    }

    override suspend fun getWallpaperColors(
        bitmap: Bitmap,
        cropHints: Map<Point, Rect>?,
    ): WallpaperColors? {
        return wallpaperManager.getWallpaperColors(bitmap, cropHints)
    }

    override fun getWallpaperColors(screen: Screen): WallpaperColors? {
        return wallpaperManager.getWallpaperColors(
            if (screen == Screen.LOCK_SCREEN) {
                FLAG_LOCK
            } else {
                FLAG_SYSTEM
            }
        )
    }

    fun WallpaperDestination.asString(): String {
        return when (this) {
            BOTH -> SCREEN_ALL
            HOME -> SCREEN_HOME
            LOCK -> SCREEN_LOCK
        }
    }

    /**
     * Adjusts cropHints for parallax effect.
     *
     * [WallpaperCropUtils.calculateCropRect] calculates based on the scaled size, the scale depends
     * on the view size hosting the preview and the wallpaper zoom of the preview on that view,
     * whereas the rest of multi-crop is based on full wallpaper size. So scaled back at the end.
     *
     * If [CropSizeModel] is null, returns the original cropHint without parallax.
     *
     * @param wallpaperSize full wallpaper image size.
     */
    private fun FullPreviewCropModel.adjustCropForParallax(wallpaperSize: Point): Rect {
        return cropSizeModel?.let {
            WallpaperCropUtils.calculateCropRect(
                    context,
                    it.hostViewSize,
                    it.cropViewSize,
                    wallpaperSize,
                    cropHint,
                    it.wallpaperZoom,
                    /* cropExtraWidth= */ true,
                )
                .apply {
                    scale(1f / it.wallpaperZoom)
                    if (right > wallpaperSize.x) right = wallpaperSize.x
                    if (bottom > wallpaperSize.y) bottom = wallpaperSize.y
                }
        } ?: cropHint
    }

    private suspend fun Asset.getStreamOrFromBitmap(bitmap: Bitmap): InputStream? =
        suspendCancellableCoroutine { k: CancellableContinuation<InputStream?> ->
            if (this is StreamableAsset) {
                if (exifOrientation != ExifInterface.ORIENTATION_NORMAL) {
                    k.resumeWith(Result.success(BitmapUtils.bitmapToInputStream(bitmap)))
                } else {
                    fetchInputStream { k.resumeWith(Result.success(it)) }
                }
            } else {
                k.resumeWith(Result.success(null))
            }
        }

    override fun addOnColorsChangedListener(
        listener: (WallpaperColors?, Int) -> Unit,
        handler: Handler,
    ) {
        wallpaperManager.addOnColorsChangedListener(listener, handler)
    }

    override fun removeOnColorsChangedListener(listener: (WallpaperColors?, Int) -> Unit) {
        wallpaperManager.removeOnColorsChangedListener(listener)
    }

    companion object {
        private const val TAG = "WallpaperClientImpl"
        private const val AUTHORITY = "com.google.android.apps.wallpaper.recents"

        /** Path for making a content provider request to set the wallpaper. */
        private const val PATH_SET_WALLPAPER = "set_recent_wallpaper"
        /** Path for making a content provider request to query for the recent wallpapers. */
        private const val PATH_LIST_RECENTS = "list_recent"
        /** Path for making a content provider request to query for the thumbnail of a wallpaper. */
        private const val PATH_GET_THUMBNAIL = "thumb"

        private val BASE_URI =
            Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build()
        /** [Uri] for making a content provider request to set the wallpaper. */
        private val SET_WALLPAPER_URI = BASE_URI.buildUpon().appendPath(PATH_SET_WALLPAPER).build()
        /** [Uri] for making a content provider request to query for the recent wallpapers. */
        private val LIST_RECENTS_URI = BASE_URI.buildUpon().appendPath(PATH_LIST_RECENTS).build()
        /**
         * [Uri] for making a content provider request to query for the thumbnail of a wallpaper.
         */
        private val GET_THUMBNAIL_BASE_URI =
            BASE_URI.buildUpon().appendPath(PATH_GET_THUMBNAIL).build()

        /** Key for a parameter used to pass the wallpaper ID to/from the content provider. */
        private const val KEY_ID = "id"
        /** Key for a parameter used to pass the screen to/from the content provider. */
        private const val KEY_SCREEN = "screen"
        /** Key for a parameter used to pass the wallpaper destination to/from content provider. */
        private const val KEY_DESTINATION = "destination"
        /** Key for a parameter used to pass the screen to/from the content provider. */
        private const val KEY_SET_WALLPAPER_ENTRY_POINT = "set_wallpaper_entry_point"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val SCREEN_ALL = "all_screens"
        private const val SCREEN_HOME = "home_screen"
        private const val SCREEN_LOCK = "lock_screen"

        private const val TITLE = "title"
        /**
         * Key for a parameter used to get the placeholder color for a wallpaper from the content
         * provider.
         */
        private const val KEY_PLACEHOLDER_COLOR = "placeholder_color"
    }
}
