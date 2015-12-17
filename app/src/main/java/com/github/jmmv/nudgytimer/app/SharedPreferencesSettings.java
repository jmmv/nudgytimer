// Copyright 2015 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at:
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// License for the specific language governing permissions and limitations
// under the License.

package com.github.jmmv.nudgytimer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.jmmv.nudgytimer.lib.Settings;

import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;

/**
 * Type-safe accessors of the application-wide settings.
 */
final class SharedPreferencesSettings implements Settings {
    /** Application-wide shared preferences. */
    private final SharedPreferences sharedPreferences;

    /**
     * Constructs a new settings object.
     *
     * @param sharedPreferences The Android preferences object for persistence.
     */
    private SharedPreferencesSettings(
            final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * Constructs a new settings object based on the application context.
     *
     * @param context The application context.
     *
     * @return A settings accessor.
     */
    public static SharedPreferencesSettings createWithContext(
            final Context context) {
        return new SharedPreferencesSettings(
                PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration getPollPeriod() {
        try {
            // TODO(jmmv): Should the preference key be in R?
            final long minutes = Long.parseLong(sharedPreferences.getString(
                    "poll_period_minutes", "MISSING_DEFAULT"));
            return new Duration(minutes * DateTimeConstants.MILLIS_PER_MINUTE);
        } catch (final NumberFormatException e) {
            throw new AssertionError("Missing or invalid default value");
        }
    }
}
