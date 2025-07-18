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
package com.android.wallpaper.asset;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.android.wallpaper.module.BitmapCropper;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.picker.preview.ui.util.CropSizeUtil;
import com.android.wallpaper.util.RtlUtils;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.WallpaperCropUtils;

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interface representing an image asset.
 */
public abstract class Asset {
    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();
    /**
     * Creates and returns a placeholder Drawable instance sized exactly to the target ImageView and
     * filled completely with pixels of the provided placeholder color.
     */
    protected static Drawable getPlaceholderDrawable(
            Context context, ImageView imageView, int placeholderColor) {
        Point imageViewDimensions = getViewDimensions(imageView);
        Bitmap placeholderBitmap =
                Bitmap.createBitmap(imageViewDimensions.x, imageViewDimensions.y, Config.ARGB_8888);
        placeholderBitmap.eraseColor(placeholderColor);
        return new BitmapDrawable(context.getResources(), placeholderBitmap);
    }

    /**
     * Returns the visible height and width in pixels of the provided ImageView, or if it hasn't
     * been laid out yet, then gets the absolute value of the layout params.
     */
    private static Point getViewDimensions(View view) {
        int width = view.getWidth() > 0 ? view.getWidth() : Math.abs(view.getLayoutParams().width);
        int height = view.getHeight() > 0 ? view.getHeight()
                : Math.abs(view.getLayoutParams().height);

        return new Point(width, height);
    }

    /**
     * Decodes a bitmap sized for the destination view's dimensions off the main UI thread.
     *
     * @param targetWidth  Width of target view in physical pixels.
     * @param targetHeight Height of target view in physical pixels.
     * @param receiver     Called with the decoded bitmap or null if there was an error decoding the
     *                     bitmap.
     */
    public final void decodeBitmap(int targetWidth, int targetHeight, BitmapReceiver receiver) {
        decodeBitmap(targetWidth, targetHeight, true, receiver);
    }


    /**
     * Decodes a bitmap sized for the destination view's dimensions off the main UI thread.
     *
     * @param targetWidth  Width of target view in physical pixels.
     * @param targetHeight Height of target view in physical pixels.
     * @param hardwareBitmapAllowed if true and it's possible, we'll try to decode into a HARDWARE
     *                              bitmap
     * @param receiver     Called with the decoded bitmap or null if there was an error decoding the
     *                     bitmap.
     */
    public abstract void decodeBitmap(int targetWidth, int targetHeight,
            boolean hardwareBitmapAllowed, BitmapReceiver receiver);

    /**
     * Copies the asset file to another place.
     * @param dest  The destination file.
     */
    public void copy(File dest) {
        // no op
    }

    /**
     * Decodes a full bitmap.
     *
     * @param receiver     Called with the decoded bitmap or null if there was an error decoding the
     *                     bitmap.
     */
    public abstract void decodeBitmap(BitmapReceiver receiver);

    /**
     * For {@link #decodeBitmap(int, int, BitmapReceiver)} to use when it is done. It then call
     * the receiver with decoded bitmap in the main thread.
     *
     * @param receiver The receiver to handle decoded bitmap or null if decoding failed.
     * @param decodedBitmap The bitmap which is already decoded.
     */
    protected void decodeBitmapCompleted(BitmapReceiver receiver, Bitmap decodedBitmap) {
        new Handler(Looper.getMainLooper()).post(() -> receiver.onBitmapDecoded(decodedBitmap));
    }

    /**
     * Decodes and downscales a bitmap region off the main UI thread.
     * @param rect         Rect representing the crop region in terms of the original image's
     *                     resolution.
     * @param targetWidth  Width of target view in physical pixels.
     * @param targetHeight Height of target view in physical pixels.
     * @param shouldAdjustForRtl whether the region selected should be adjusted for RTL (that is,
     *                           the crop region will be considered starting from the right)
     * @param receiver     Called with the decoded bitmap region or null if there was an error
     */
    public abstract void decodeBitmapRegion(Rect rect, int targetWidth, int targetHeight,
            boolean shouldAdjustForRtl, BitmapReceiver receiver);

    /**
     * Calculates the raw dimensions of the asset at its original resolution off the main UI thread.
     * Avoids decoding the entire bitmap if possible to conserve memory.
     *
     * @param activity Activity in which this decoding request is made. Allows for early termination
     *                 of fetching image data and/or decoding to a bitmap. May be null, in which
     *                 case the request is made in the application context instead.
     * @param receiver Called with the decoded raw dimensions of the whole image or null if there
     *                 was an error decoding the dimensions.
     */
    public abstract void decodeRawDimensions(@Nullable Activity activity,
            DimensionsReceiver receiver);

    /**
     * Returns whether this asset has access to a separate, lower fidelity source of image data
     * (that may be able to be loaded more quickly to simulate progressive loading).
     */
    public boolean hasLowResDataSource() {
        return false;
    }

    /**
     * Loads the asset from the separate low resolution data source (if there is one) into the
     * provided ImageView with the placeholder color and bitmap transformation.
     *
     * @param transformation Bitmap transformation that can transform the thumbnail image
     *                       post-decoding.
     */
    public void loadLowResDrawable(Activity activity, ImageView imageView, int placeholderColor,
            BitmapTransformation transformation) {
        // No op
    }

    /**
     * Returns a Bitmap from the separate low resolution data source (if there is one) or
     * {@code null} otherwise.
     * This could be an I/O operation so DO NOT CALL ON UI THREAD
     */
    @WorkerThread
    @Nullable
    public Bitmap getLowResBitmap(Context context) {
        return null;
    }

    /**
     * Returns whether the asset supports rendering tile regions at varying pixel densities.
     */
    public abstract boolean supportsTiling();

    /**
     * Loads a Drawable for this asset into the provided ImageView. While waiting for the image to
     * load, first loads a ColorDrawable based on the provided placeholder color.
     *
     * @param context          Activity hosting the ImageView.
     * @param imageView        ImageView which is the target view of this asset.
     * @param placeholderColor Color of placeholder set to ImageView while waiting for image to
     *                         load.
     */
    public void loadDrawable(final Context context, final ImageView imageView,
            int placeholderColor) {
        // Transition from a placeholder ColorDrawable to the decoded bitmap when the ImageView in
        // question is empty.
        final boolean needsTransition = imageView.getDrawable() == null;
        final Drawable placeholderDrawable = new ColorDrawable(placeholderColor);
        if (needsTransition) {
            imageView.setImageDrawable(placeholderDrawable);
        }

        // Set requested height and width to the either the actual height and width of the view in
        // pixels, or if it hasn't been laid out yet, then to the absolute value of the layout
        // params.
        int width = imageView.getWidth() > 0
                ? imageView.getWidth()
                : Math.abs(imageView.getLayoutParams().width);
        int height = imageView.getHeight() > 0
                ? imageView.getHeight()
                : Math.abs(imageView.getLayoutParams().height);

        decodeBitmap(width, height, new BitmapReceiver() {
            @Override
            public void onBitmapDecoded(Bitmap bitmap) {
                if (!needsTransition) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }

                Resources resources = context.getResources();

                Drawable[] layers = new Drawable[2];
                layers[0] = placeholderDrawable;
                layers[1] = new BitmapDrawable(resources, bitmap);

                TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                transitionDrawable.setCrossFadeEnabled(true);

                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(resources.getInteger(
                        android.R.integer.config_shortAnimTime));
            }
        });
    }

    /**
     * Loads a Drawable for this asset into the provided ImageView, providing a crossfade transition
     * with the given duration from the Drawable previously set on the ImageView.
     *
     * @param context                  Activity hosting the ImageView.
     * @param imageView                ImageView which is the target view of this asset.
     * @param transitionDurationMillis Duration of the crossfade, in milliseconds.
     * @param drawableLoadedListener   Listener called once the transition has begun.
     * @param placeholderColor         Color of the placeholder if the provided ImageView is empty
     *                                 before the
     */
    public void loadDrawableWithTransition(
            final Context context,
            final ImageView imageView,
            final int transitionDurationMillis,
            @Nullable final DrawableLoadedListener drawableLoadedListener,
            int placeholderColor) {
        Point imageViewDimensions = getViewDimensions(imageView);

        // Transition from a placeholder ColorDrawable to the decoded bitmap when the ImageView in
        // question is empty.
        boolean needsPlaceholder = imageView.getDrawable() == null;
        if (needsPlaceholder) {
            imageView.setImageDrawable(
                    getPlaceholderDrawable(context, imageView, placeholderColor));
        }

        decodeBitmap(imageViewDimensions.x, imageViewDimensions.y, new BitmapReceiver() {
            @Override
            public void onBitmapDecoded(Bitmap bitmap) {
                final Resources resources = context.getResources();

                centerCropBitmap(bitmap, imageView, new BitmapReceiver() {
                    @Override
                    public void onBitmapDecoded(@Nullable Bitmap newBitmap) {
                        Drawable[] layers = new Drawable[2];
                        Drawable existingDrawable = imageView.getDrawable();

                        if (existingDrawable instanceof TransitionDrawable) {
                            // Take only the second layer in the existing TransitionDrawable so
                            // we don't keep
                            // around a reference to older layers which are no longer shown (this
                            // way we avoid a
                            // memory leak).
                            TransitionDrawable existingTransitionDrawable =
                                    (TransitionDrawable) existingDrawable;
                            int id = existingTransitionDrawable.getId(1);
                            layers[0] = existingTransitionDrawable.findDrawableByLayerId(id);
                        } else {
                            layers[0] = existingDrawable;
                        }
                        layers[1] = new BitmapDrawable(resources, newBitmap);

                        TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                        transitionDrawable.setCrossFadeEnabled(true);

                        imageView.setImageDrawable(transitionDrawable);
                        transitionDrawable.startTransition(transitionDurationMillis);

                        if (drawableLoadedListener != null) {
                            drawableLoadedListener.onDrawableLoaded();
                        }
                    }
                });
            }
        });
    }

    /**
     * Loads the image for this asset into the provided ImageView which is used for the preview.
     * While waiting for the image to load, first loads a ColorDrawable based on the provided
     * placeholder color.
     *
     * @param activity         Activity hosting the ImageView.
     * @param imageView        ImageView which is the target view of this asset.
     * @param placeholderColor Color of placeholder set to ImageView while waiting for image to
     *                         load.
     * @param offsetToStart    true to let the preview show from the start of the image, false to
     *                         center-aligned to the image.
     */
    public void loadPreviewImage(Activity activity, ImageView imageView, int placeholderColor,
            boolean offsetToStart) {
        loadPreviewImage(activity, imageView, placeholderColor, offsetToStart, null);
    }

    /**
     * Loads the image for this asset into the provided ImageView which is used for the preview.
     * While waiting for the image to load, first loads a ColorDrawable based on the provided
     * placeholder color.
     *
     * @param activity         Activity hosting the ImageView.
     * @param imageView        ImageView which is the target view of this asset.
     * @param placeholderColor Color of placeholder set to ImageView while waiting for image to
     *                         load.
     * @param offsetToStart    true to let the preview show from the start of the image, false to
     *                         center-aligned to the image.
     * @param cropHints        A Map of display size to crop rect
     */
    public void loadPreviewImage(Activity activity, ImageView imageView, int placeholderColor,
            boolean offsetToStart, @Nullable Map<Point, Rect> cropHints) {
        boolean needsTransition = imageView.getDrawable() == null;
        Drawable placeholderDrawable = new ColorDrawable(placeholderColor);
        if (needsTransition) {
            imageView.setImageDrawable(placeholderDrawable);
        }

        decodeRawDimensions(activity, dimensions -> {
            // TODO (b/286404249): A proper fix here would be to find out why the
            //  leak happens in first place
            if (activity.isDestroyed()) {
                return;
            }
            if (dimensions == null) {
                loadDrawable(activity, imageView, placeholderColor);
                return;
            }
            boolean isRtl = RtlUtils.isRtl(activity);
            Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
            Point screenSize = ScreenSizeCalculator.getInstance().getScreenSize(defaultDisplay);
            Rect visibleRawWallpaperRect =
                    WallpaperCropUtils.calculateVisibleRect(dimensions, screenSize);
            if (cropHints != null && cropHints.containsKey(screenSize)) {
                visibleRawWallpaperRect = CropSizeUtil.INSTANCE.fitCropRectToLayoutDirection(
                        cropHints.get(screenSize), screenSize, RtlUtils.isRtl(activity));
                // For multi-crop, the visibleRawWallpaperRect above is already the exact size of
                // the part of wallpaper we should show on the screen, turning off the old RTL
                // logic by assigning false.
                isRtl = false;
            }

            // TODO(b/264234793): Make offsetToStart general support or for the specific asset.
            adjustCropRect(activity, dimensions, visibleRawWallpaperRect, offsetToStart);

            float scale = (float) visibleRawWallpaperRect.width() / screenSize.x;

            BitmapCropper bitmapCropper = InjectorProvider.getInjector().getBitmapCropper();
            bitmapCropper.cropAndScaleBitmap(this, scale, visibleRawWallpaperRect,
                    isRtl,
                    new BitmapCropper.Callback() {
                        @Override
                        public void onBitmapCropped(Bitmap croppedBitmap) {
                            // Since the size of the cropped bitmap may not exactly the same with
                            // image view(maybe has 1px or 2px difference),
                            // so set CENTER_CROP to let the bitmap to fit the image view.
                            if (!activity.isDestroyed()) {
                                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                if (!needsTransition) {
                                    imageView.setImageBitmap(croppedBitmap);
                                    return;
                                }

                                Resources resources = activity.getResources();

                                Drawable[] layers = new Drawable[2];
                                layers[0] = placeholderDrawable;
                                layers[1] = new BitmapDrawable(resources, croppedBitmap);

                                TransitionDrawable transitionDrawable = new
                                        TransitionDrawable(layers);
                                transitionDrawable.setCrossFadeEnabled(true);

                                imageView.setImageDrawable(transitionDrawable);
                                transitionDrawable.startTransition(resources.getInteger(
                                        android.R.integer.config_shortAnimTime));
                            }
                        }

                        @Override
                        public void onError(@Nullable Throwable e) {
                            if (!activity.isDestroyed()) {
                                loadDrawable(activity, imageView, placeholderColor);
                            }
                        }
                    });
        });
    }

    /**
     * Interface for receiving decoded Bitmaps.
     */
    public interface BitmapReceiver {

        /**
         * Called with a decoded Bitmap object or null if there was an error decoding the bitmap.
         */
        void onBitmapDecoded(@Nullable Bitmap bitmap);
    }

    /**
     * Interface for receiving raw asset dimensions.
     */
    public interface DimensionsReceiver {

        /**
         * Called with raw dimensions of asset or null if the asset is unable to decode the raw
         * dimensions.
         *
         * @param dimensions Dimensions as a Point where width is represented by "x" and height by
         *                   "y".
         */
        void onDimensionsDecoded(@Nullable Point dimensions);
    }

    /**
     * Interface for being notified when a drawable has been loaded.
     */
    public interface DrawableLoadedListener {
        void onDrawableLoaded();
    }

    protected void adjustCropRect(Context context, Point assetDimensions, Rect cropRect,
            boolean offsetToStart) {
        WallpaperCropUtils.adjustCropRect(context, cropRect, true /* zoomIn */);
    }

    /**
     * Returns a copy of the given bitmap which is center cropped and scaled
     * to fit in the given ImageView and the thread runs on ExecutorService.
     */
    public void centerCropBitmap(Bitmap bitmap, View view, BitmapReceiver bitmapReceiver) {
        Point imageViewDimensions = getViewDimensions(view);
        sExecutorService.execute(() -> {
            int measuredWidth = imageViewDimensions.x;
            int measuredHeight = imageViewDimensions.y;

            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            float scale = Math.min(
                    (float) bitmapWidth / measuredWidth,
                    (float) bitmapHeight / measuredHeight);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap, Math.round(bitmapWidth / scale), Math.round(bitmapHeight / scale),
                    true);

            int horizontalGutterPx = Math.max(0, (scaledBitmap.getWidth() - measuredWidth) / 2);
            int verticalGutterPx = Math.max(0, (scaledBitmap.getHeight() - measuredHeight) / 2);
            Bitmap result = Bitmap.createBitmap(
                    scaledBitmap,
                    horizontalGutterPx,
                    verticalGutterPx,
                    scaledBitmap.getWidth() - (2 * horizontalGutterPx),
                    scaledBitmap.getHeight() - (2 * verticalGutterPx));
            decodeBitmapCompleted(bitmapReceiver, result);
        });
    }
}
