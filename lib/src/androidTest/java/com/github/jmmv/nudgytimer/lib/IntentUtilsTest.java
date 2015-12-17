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

package com.github.jmmv.nudgytimer.lib;

import android.content.Intent;

import junit.framework.TestCase;

import org.joda.time.Interval;

public class IntentUtilsTest extends TestCase {
    public void testGetFromIntentAndPutInIntent() throws Exception {
        final Interval interval = new Interval(5, 10);

        final Intent intent = new Intent();
        IntentUtils.putExtra(intent, interval);
        assertEquals(interval, IntentUtils.getIntervalExtra(intent));
    }

    public void testGetFromIntent_BadIntent_Incomplete() throws Exception {
        final Intent intent = new Intent();

        try {
            IntentUtils.getIntervalExtra(intent);
            fail("BadIntentException not thrown");
        } catch (final BadIntentException e) {
            assertTrue(e.getMessage().matches(".*missing.*startMillis.*"));
        }

        //noinspection SpellCheckingInspection
        intent.putExtra(
                "com.github.jmmv.nudgytimer.lib.INTERVAL_START_MILLIS_ID",
                10L);
        try {
            IntentUtils.getIntervalExtra(intent);
            fail("BadIntentException not thrown");
        } catch (final BadIntentException e) {
            assertTrue(e.getMessage().matches(".*missing.*endMillis.*"));
        }
    }

    public void testGetFromIntent_BadIntent_BadData() throws Exception {
        final Intent intent = new Intent();
        //noinspection SpellCheckingInspection
        intent.putExtra(
                "com.github.jmmv.nudgytimer.lib.INTERVAL_START_MILLIS_ID",
                10L);
        //noinspection SpellCheckingInspection
        intent.putExtra(
                "com.github.jmmv.nudgytimer.lib.INTERVAL_END_MILLIS_ID",
                -10L);
        try {
            IntentUtils.getIntervalExtra(intent);
            fail("BadIntentException not thrown");
        } catch (final BadIntentException e) {
            assertTrue(e.getMessage().matches(".*Backwards interval.*"));
        }
    }
}
