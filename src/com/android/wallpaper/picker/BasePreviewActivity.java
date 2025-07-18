/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.picker;

import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;

import com.android.wallpaper.R;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.logging.UserEventLogger;

/**
 * Abstract base class for a wallpaper full-screen preview activity.
 */
public abstract class BasePreviewActivity extends BaseActivity {
    public static final String EXTRA_WALLPAPER_INFO =
            "com.android.wallpaper.picker.wallpaper_info";
    public static final String EXTRA_VIEW_AS_HOME =
            "com.android.wallpaper.picker.view_as_home";
    public static final String IS_ASSET_ID_PRESENT =
            "com.android.wallpaper.picker.asset_id_present";
    public static final String IS_NEW_TASK =
            "com.android.wallpaper.picker.new_task";
    public static final String SHOULD_CATEGORY_REFRESH =
            "com.android.wallpaper.picker.should_category_refresh";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector injector = InjectorProvider.getInjector();
        UserEventLogger mUserEventLogger = injector.getUserEventLogger();
        getWindow().setColorMode(ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
        setTheme(R.style.WallpaperTheme);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        // Check the launching intent's action to figure out the caller is from other application
        // and log its launch source.
        if (getIntent() != null && getIntent().getAction() != null) {
            mUserEventLogger.logAppLaunched(getIntent());
        }
    }

    /** Allows the current activity to be full screen. */
    protected void enableFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), /* decorFitsSystemWindows= */ false);

        // Window insets are set in the PreviewFragment#onCreateView method.
    }
}
