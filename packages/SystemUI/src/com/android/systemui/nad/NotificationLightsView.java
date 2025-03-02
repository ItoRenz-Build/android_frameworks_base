/*
* Copyright (C) 2019 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.android.systemui.nad;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.WallpaperInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.palette.graphics.Palette;

import com.android.settingslib.Utils;
import com.android.systemui.R;

public class NotificationLightsView extends RelativeLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "NotificationLightsView";
    private static final String CANCEL_NOTIFICATION_PULSE_ACTION = "cancel_notification_pulse";
    private ValueAnimator mLightAnimator;

    public NotificationLightsView(Context context) {
        this(context, null);
    }

    public NotificationLightsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (DEBUG) Log.d(TAG, "new");
    }

    public void stopAnimateNotification() {
        if (mLightAnimator != null) {
            mLightAnimator.end();
            mLightAnimator = null;
        }
    }

    public void animateNotification() {
        animateNotificationWithColor(getNotificationLightsColor());
    }

    public int getNotificationLightsColor() {
        int color = 0xFFFFFFFF;
        int lightColor = getlightColor();
        int customColor = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_PULSE_COLOR, 0xFFFFFFFF,
                UserHandle.USER_CURRENT);
        int blend = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_BLEND_COLOR, 0xFFFFFFFF,
                UserHandle.USER_CURRENT);
        switch (lightColor) {
            case 1: // Wallpaper
                try {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                    WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
                    if (wallpaperInfo == null) { // if not a live wallpaper
                        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                        Bitmap bitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                        if (bitmap != null) { // if wallpaper is not blank
                            Palette p = Palette.from(bitmap).generate();
                            int wallColor = p.getDominantColor(color);
                            if (color != wallColor)
                                color = wallColor;
                        }
                    }
                } catch (Exception e) { /* nothing to do, will use fallback */ }
                break;
            case 2: // Accent
                color = Utils.getColorAccentDefaultColor(getContext());
                break;
            case 3: // Custom
                color = customColor;
                break;
            case 4: // Blend
                color = mixColors(customColor, blend);
                break;
            case 5: // Blend
                color = randomColor();
                break;
            default: // White
                color = 0xFFFFFFFF;
        }
        return color;
    }

    public int randomColor() {
        int red = (int) (0xff * Math.random());
        int green = (int) (0xff * Math.random());
        int blue = (int) (0xff * Math.random());
        return Color.argb(255, red, green, blue);
    }

    private int mixColors(int color1, int color2) {
        int[] rgb1 = colorToRgb(color1);
        int[] rgb2 = colorToRgb(color2);

        rgb1[0] = mixedValue(rgb1[0], rgb2[0]);
        rgb1[1] = mixedValue(rgb1[1], rgb2[1]);
        rgb1[2] = mixedValue(rgb1[2], rgb2[2]);
        rgb1[3] = mixedValue(rgb1[3], rgb2[3]);

        return rgbToColor(rgb1);
    }

    private int[] colorToRgb(int color) {
        int[] rgb = {(color & 0xFF000000) >> 24, (color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, (color & 0xFF)};
        return rgb;
    }

    private int rgbToColor(int[] rgb) {
        return (rgb[0] << 24) + (rgb[1] << 16) + (rgb[2] << 8) + rgb[3];
    }

    private int mixedValue(int val1, int val2) {
        return (int)Math.min((val1 + val2), 255f);
    }

    public int getlightColor() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
               Settings.System.NOTIFICATION_PULSE_COLOR_MODE, 0,
               UserHandle.USER_CURRENT);
    }

    public void animateNotificationWithColor(int color) {
        ContentResolver resolver = mContext.getContentResolver();
        int duration = Settings.System.getIntForUser(resolver,
                Settings.System.NOTIFICATION_PULSE_DURATION, 2,
                UserHandle.USER_CURRENT) * 1000; // seconds to ms
        int repeats = Settings.System.getIntForUser(resolver,
                Settings.System.NOTIFICATION_PULSE_REPEATS, 0,
                UserHandle.USER_CURRENT);
        boolean directionIsRestart = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_REPEAT_DIRECTION, 0,
                UserHandle.USER_CURRENT) != 1;

        int style = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_LAYOUT, 0,
                UserHandle.USER_CURRENT);
        int lightcolor = getlightColor();

        ImageView leftViewFaded = (ImageView) findViewById(R.id.notification_animation_left_faded);
        ImageView rightViewFaded = (ImageView) findViewById(R.id.notification_animation_right_faded);
        ImageView leftViewSolid = (ImageView) findViewById(R.id.notification_animation_left_solid);
        ImageView rightViewSolid = (ImageView) findViewById(R.id.notification_animation_right_solid);
        if (lightcolor == 6) {
            leftViewFaded.setColorFilter(randomColor());
            rightViewFaded.setColorFilter(randomColor());
        } else {
            leftViewFaded.setColorFilter(color);
            rightViewFaded.setColorFilter(color);
        }
        leftViewFaded.setVisibility(style == 0 ? View.VISIBLE : View.GONE);
        rightViewFaded.setVisibility(style == 0 ? View.VISIBLE : View.GONE);
        if (lightcolor == 6) {
            leftViewSolid.setColorFilter(randomColor());
            rightViewSolid.setColorFilter(randomColor());
        } else {
           leftViewSolid.setColorFilter(color);
           rightViewSolid.setColorFilter(color);
        }
        leftViewSolid.setVisibility(style == 1 ? View.VISIBLE : View.GONE);
        rightViewSolid.setVisibility(style == 1 ? View.VISIBLE : View.GONE);
        mLightAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 2.0f});
        mLightAnimator.setDuration(duration);
        mLightAnimator.setRepeatCount(repeats == 0 ?
                ValueAnimator.INFINITE : repeats - 1);
        mLightAnimator.setRepeatMode(directionIsRestart ? ValueAnimator.RESTART : ValueAnimator.REVERSE);
        if (repeats != 0) {
            mLightAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) { /* do nothing */ }
                @Override
                public void onAnimationRepeat(Animator animation) { /* do nothing */ }
                @Override
                public void onAnimationStart(Animator animation) { /* do nothing */ }
                @Override
                public void onAnimationEnd(Animator animation) {
                    Settings.System.putIntForUser(resolver,
                            Settings.System.AOD_NOTIFICATION_PULSE_ACTIVATED, 0,
                            UserHandle.USER_CURRENT);
                    Settings.System.putIntForUser(resolver,
                            Settings.System.AOD_NOTIFICATION_PULSE_TRIGGER, 0,
                            UserHandle.USER_CURRENT);
                }
            });
        }
        mLightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (DEBUG) Log.d(TAG, "onAnimationUpdate");
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                leftViewFaded.setScaleY(progress);
                rightViewFaded.setScaleY(progress);
                leftViewSolid.setScaleY(progress);
                rightViewSolid.setScaleY(progress);
                float alpha = 1.0f;
                if (progress <= 0.3f) {
                    alpha = progress / 0.3f;
                } else if (progress >= 1.0f) {
                    alpha = 2.0f - progress;
                }
                leftViewFaded.setAlpha(alpha);
                rightViewFaded.setAlpha(alpha);
                leftViewSolid.setAlpha(alpha);
                rightViewSolid.setAlpha(alpha);
            }
        });
        if (DEBUG) Log.d(TAG, "start");
        mLightAnimator.start();
    }
}
