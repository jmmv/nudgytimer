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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.github.jmmv.nudgytimer.lib.Settings;

import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;

public class SharedPreferencesSettingsTest extends AndroidTestCase {
    /** Persistent settings on an isolated context. */
    private Settings settings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setContext(new RenamingDelegatingContext(getContext(), "test_"));

        PreferenceManager.setDefaultValues(getContext(), R.xml.preferences,
                                           false);
        settings = SharedPreferencesSettings.createWithContext(getContext());
    }

    public void testGetPollPeriod_Default() {
        assertEquals(new Duration(15 * DateTimeConstants.MILLIS_PER_MINUTE),
                     settings.getPollPeriod());
    }

    public void testGetPollPeriod_Overridden() {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("poll_period_minutes", "10");
        editor.apply();

        assertEquals(new Duration(10 * DateTimeConstants.MILLIS_PER_MINUTE),
                     settings.getPollPeriod());
    }
}
