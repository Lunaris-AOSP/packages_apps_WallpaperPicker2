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
package com.android.wallpaper.picker.common.preview.ui.binder

import android.app.WallpaperColors
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.systemui.shared.clocks.shared.model.ClockPreviewConstants
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_HIGHLIGHT_QUICK_AFFORDANCES
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_INITIALLY_SELECTED_SLOT_ID
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.common.preview.ui.viewmodel.BasePreviewViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.SurfaceViewUtils
import kotlin.coroutines.resume
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Based on com/android/wallpaper/picker/preview/ui/binder/WorkspacePreviewBinder.kt, with a
// subset of the original bind methods and currently without wallpaper colors updates.
object WorkspacePreviewBinder {
    fun bind(
        surfaceView: SurfaceView,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        workspaceCallbackBinder: WorkspaceCallbackBinder,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        var surfaceCallback: SurfaceViewUtils.SurfaceCallback? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                surfaceCallback =
                    bindSurface(
                        surfaceView = surfaceView,
                        viewModel = viewModel,
                        colorUpdateViewModel = colorUpdateViewModel,
                        workspaceCallbackBinder = workspaceCallbackBinder,
                        screen = screen,
                        previewUtils = getPreviewUtils(screen, viewModel.basePreviewViewModel),
                        deviceDisplayType = deviceDisplayType,
                        lifecycleOwner = lifecycleOwner,
                        clockViewFactory = clockViewFactory,
                    )
                surfaceView.setZOrderMediaOverlay(true)
                surfaceView.holder.addCallback(surfaceCallback)
            }
            // When OnDestroy, release the surface
            surfaceCallback?.let {
                surfaceView.holder.removeCallback(it)
                surfaceCallback = null
            }
        }
    }

    /**
     * Create a surface callback that binds the surface when surface created. Note that we return
     * the surface callback reference so that we can remove the callback from the surface when the
     * screen is destroyed.
     */
    private fun bindSurface(
        surfaceView: SurfaceView,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        workspaceCallbackBinder: WorkspaceCallbackBinder,
        screen: Screen,
        previewUtils: PreviewUtils,
        deviceDisplayType: DeviceDisplayType,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ): SurfaceViewUtils.SurfaceCallback {
        return object : SurfaceViewUtils.SurfaceCallback {

            var job: Job? = null
            var previewDisposableHandle: DisposableHandle? = null

            override fun surfaceCreated(holder: SurfaceHolder) {
                job =
                    lifecycleOwner.lifecycleScope.launch {
                        renderWorkspacePreview(
                                surfaceView = surfaceView,
                                screen = screen,
                                previewUtils = previewUtils,
                                displayId =
                                    viewModel.basePreviewViewModel.getDisplayId(deviceDisplayType),
                            )
                            ?.let { workspaceCallback ->
                                workspaceCallbackBinder.bind(
                                    workspaceCallback = workspaceCallback,
                                    viewModel = viewModel.customizationOptionsViewModel,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    screen = screen,
                                    clockViewFactory = clockViewFactory,
                                )
                            }
                    }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                job?.cancel()
                job = null
                previewDisposableHandle?.dispose()
                previewDisposableHandle = null
            }
        }
    }

    private suspend fun renderWorkspacePreview(
        surfaceView: SurfaceView,
        screen: Screen,
        previewUtils: PreviewUtils,
        displayId: Int,
        wallpaperColors: WallpaperColors? = null,
    ): Message? {
        var workspaceCallback: Message? = null
        if (previewUtils.supportsPreview()) {
            // surfaceView.width and surfaceFrame.width here can be different, one represents the
            // size of the view and the other represents the size of the surface. When requesting a
            // preview, make sure to specify the width and height in the bundle so we are using the
            // surface size and not the view size.
            val surfacePosition = surfaceView.holder.surfaceFrame
            val extras =
                bundleOf(
                        Pair(SurfaceViewUtils.KEY_DISPLAY_ID, displayId),
                        Pair(SurfaceViewUtils.KEY_VIEW_WIDTH, surfacePosition.width()),
                        Pair(SurfaceViewUtils.KEY_VIEW_HEIGHT, surfacePosition.height()),
                    )
                    .apply {
                        if (screen == Screen.LOCK_SCREEN) {
                            putBoolean(ClockPreviewConstants.KEY_HIDE_CLOCK, true)
                            putString(KEY_INITIALLY_SELECTED_SLOT_ID, SLOT_ID_BOTTOM_START)
                            putBoolean(KEY_HIGHLIGHT_QUICK_AFFORDANCES, false)
                        }
                    }

            wallpaperColors?.let {
                extras.putParcelable(SurfaceViewUtils.KEY_WALLPAPER_COLORS, wallpaperColors)
            }
            val request = SurfaceViewUtils.createSurfaceViewRequest(surfaceView, extras)
            workspaceCallback = suspendCancellableCoroutine { continuation ->
                previewUtils.renderPreview(
                    request,
                    object : PreviewUtils.WorkspacePreviewCallback {
                        override fun onPreviewRendered(resultBundle: Bundle?) {
                            if (resultBundle != null) {
                                SurfaceViewUtils.getSurfacePackage(resultBundle).apply {
                                    if (this != null) {
                                        surfaceView.setChildSurfacePackage(this)
                                    } else {
                                        Log.w(
                                            TAG,
                                            "Result bundle from rendering preview does not contain " +
                                                "a child surface package.",
                                        )
                                    }
                                }
                                continuation.resume(SurfaceViewUtils.getCallback(resultBundle))
                            } else {
                                Log.w(TAG, "Result bundle from rendering preview is null.")
                                continuation.resume(null)
                            }
                        }
                    },
                )
            }
        }
        return workspaceCallback
    }

    private fun getPreviewUtils(
        screen: Screen,
        previewViewModel: BasePreviewViewModel,
    ): PreviewUtils =
        when (screen) {
            Screen.HOME_SCREEN -> {
                previewViewModel.homePreviewUtils
            }
            Screen.LOCK_SCREEN -> {
                previewViewModel.lockPreviewUtils
            }
        }

    const val TAG = "WorkspacePreviewBinder"
}
