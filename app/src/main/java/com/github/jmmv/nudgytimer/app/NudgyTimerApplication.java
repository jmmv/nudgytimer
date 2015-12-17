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

import android.app.Application;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.github.jmmv.nudgytimer.lib.ActivityTrigger;
import com.github.jmmv.nudgytimer.lib.Settings;
import com.github.jmmv.nudgytimer.lib.SqliteTracker;
import com.github.jmmv.nudgytimer.lib.StoreHelper;
import com.github.jmmv.nudgytimer.lib.Tracker;

/**
 * Global application state.
 */
@SuppressWarnings("WeakerAccess")  // Android Studio bug #197320
public final class NudgyTimerApplication extends Application {
    /** Application-wide periodic activity trigger programmer. */
    private ActivityTrigger activityTrigger;

    /** Application-wide settings accessor. */
    private Settings settings;

    /** Application-wide instance of the events tracker. */
    private Tracker tracker;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings = SharedPreferencesSettings.createWithContext(this);

        final StoreHelper storeHelper = StoreHelper.newOnDisk(getBaseContext());
        tracker = new SqliteTracker(storeHelper.getWritableDatabase());

        activityTrigger = new ActivityTrigger(this, settings,
                                              EnterActivity.class);
    }

    /** Returns the application-wide activityTrigger programmer. */
    public @NonNull ActivityTrigger getActivityTrigger() {
        if (BuildConfig.DEBUG && activityTrigger == null) {
            throw new AssertionError("onCreate not yet called");
        }
        return activityTrigger;
    }

    /** Returns the application-wide settings accessor. */
    public @NonNull Settings getSettings() {
        if (BuildConfig.DEBUG && activityTrigger == null) {
            throw new AssertionError("onCreate not yet called");
        }
        return settings;
    }

    /** Returns the application-wide instance of the events tracker. */
    public @NonNull Tracker getTracker() {
        if (BuildConfig.DEBUG && activityTrigger == null) {
            throw new AssertionError("onCreate not yet called");
        }
        return tracker;
    }
}
