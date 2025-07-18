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
package com.android.wallpaper.picker.preview.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.binder.CropWallpaperButtonBinder
import com.android.wallpaper.picker.preview.ui.binder.FullWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.binder.PreviewTooltipBinder
import com.android.wallpaper.picker.preview.ui.binder.WorkspacePreviewBinder
import com.android.wallpaper.picker.preview.ui.transition.ChangeScaleAndPosition
import com.android.wallpaper.picker.preview.ui.util.AnimationUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope

/** Shows full preview of user selected wallpaper for cropping, zooming and positioning. */
@AndroidEntryPoint(AppbarFragment::class)
class FullPreviewFragment : Hilt_FullPreviewFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils

    private lateinit var currentView: View

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()
    private val isFirstBindingDeferred = CompletableDeferred<Boolean>()

    private var useLightToolbarOverride = false
    private var navigateUpListener: NavController.OnDestinationChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = AnimationUtil.getFastFadeInTransition()
        returnTransition = AnimationUtil.getFastFadeOutTransition()
        sharedElementEnterTransition = ChangeScaleAndPosition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        currentView = inflater.inflate(R.layout.fragment_full_preview, container, false)

        navigateUpListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.smallPreviewFragment) {
                    wallpaperPreviewViewModel.handleBackPressed()
                    currentView.findViewById<View>(R.id.crop_wallpaper_button)?.isVisible = false
                    currentView.findViewById<View>(R.id.full_preview_tooltip_stub)?.isVisible =
                        false
                    // When navigate up back to small preview, move previews up app window for
                    // smooth shared element transition. It's the earliest timing to do this, it'll
                    // be to late in transition started callback.
                    currentView
                        .requireViewById<SurfaceView>(R.id.wallpaper_surface)
                        .setZOrderOnTop(true)
                    currentView
                        .requireViewById<SurfaceView>(R.id.workspace_surface)
                        .setZOrderOnTop(true)
                }
            }
        navigateUpListener?.let { findNavController().addOnDestinationChangedListener(it) }

        setUpToolbar(currentView, true, true)

        val previewCard: CardView = currentView.requireViewById(R.id.preview_card)
        ViewCompat.setTransitionName(
            previewCard,
            SmallPreviewFragment.FULL_PREVIEW_SHARED_ELEMENT_ID,
        )

        FullWallpaperPreviewBinder.bind(
            applicationContext = appContext,
            view = currentView,
            viewModel = wallpaperPreviewViewModel,
            transition = sharedElementEnterTransition as? Transition,
            displayUtils = displayUtils,
            mainScope = mainScope,
            lifecycleOwner = viewLifecycleOwner,
            savedInstanceState = savedInstanceState,
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBindingDeferred = isFirstBindingDeferred,
        ) { isFullScreen ->
            useLightToolbarOverride = isFullScreen
            setUpToolbar(view)
        }

        CropWallpaperButtonBinder.bind(
            button = currentView.requireViewById(R.id.crop_wallpaper_button),
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        ) {
            wallpaperPreviewViewModel.handleBackPressed()
            findNavController().popBackStack()
        }

        WorkspacePreviewBinder.bindFullWorkspacePreview(
            surface = currentView.requireViewById(R.id.workspace_surface),
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )

        PreviewTooltipBinder.bindFullPreviewTooltip(
            tooltipStub = currentView.requireViewById(R.id.full_preview_tooltip_stub),
            viewModel = wallpaperPreviewViewModel.fullTooltipViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )

        return currentView
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        isFirstBindingDeferred.complete(savedInstanceState == null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigateUpListener?.let { findNavController().removeOnDestinationChangedListener(it) }
    }

    // TODO(b/291761856): Use real string
    override fun getDefaultTitle(): CharSequence {
        return ""
    }

    override fun getToolbarTextColor(): Int {
        return if (useLightToolbarOverride) {
            ContextCompat.getColor(requireContext(), android.R.color.system_on_primary_light)
        } else {
            ContextCompat.getColor(requireContext(), R.color.system_on_surface)
        }
    }

    override fun isStatusBarLightText(): Boolean {
        return requireContext().resources.getBoolean(R.bool.isFragmentStatusBarLightText) or
            useLightToolbarOverride
    }
}
