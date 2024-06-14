/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package WindowPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Window;
import android.view.WindowInsets;

import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;

import com.google.android.material.internal.EdgeToEdgeUtils;

/**
 * Helper that saves the current window preferences for the Catalog.
 */
public class WindowPreferencesManager {

    private static final String PREFERENCES_NAME = "window_preferences";
    private static final String KEY_EDGE_TO_EDGE_ENABLED = "edge_to_edge_enabled";

    private final Context context;
    private final OnApplyWindowInsetsListener listener;

    public WindowPreferencesManager(Context context) {
        this.context = context;
        this.listener =
                (v, insets) -> {
                    if (v.getResources().getConfiguration().orientation
                            != Configuration.ORIENTATION_LANDSCAPE) {
                        return insets;
                    }
                    v.setPadding(
                            insets.getInsets(WindowInsets.Type.systemBars()).left,
                            0,
                            insets.getInsets(WindowInsets.Type.systemBars()).right,
                            insets.getInsets(WindowInsets.Type.systemBars()).bottom);
                    return insets;
                };
    }

    @SuppressWarnings("ApplySharedPref")
    public void toggleEdgeToEdgeEnabled() {
        getSharedPreferences()
                .edit()
                .putBoolean(KEY_EDGE_TO_EDGE_ENABLED, !isEdgeToEdgeEnabled())
                .commit();
    }

    public boolean isEdgeToEdgeEnabled() {
        return getSharedPreferences()
                .getBoolean(KEY_EDGE_TO_EDGE_ENABLED, true);
    }

    @SuppressWarnings("RestrictTo")
    public void applyEdgeToEdgePreference(Window window) {
        EdgeToEdgeUtils.applyEdgeToEdge(window, isEdgeToEdgeEnabled());
        ViewCompat.setOnApplyWindowInsetsListener(
                window.getDecorView(), isEdgeToEdgeEnabled() ? listener : null);
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}