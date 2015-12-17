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

import org.joda.time.Interval;

/**
 * Utilities to deal with intents.
 */
public final class IntentUtils {
    /** Identifier for the start time field stored in an intent. */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String START_MILLIS_ID =
            "com.github.jmmv.nudgytimer.lib.INTERVAL_START_MILLIS_ID";

    /** Identifier for the end time field stored in an intent. */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String END_MILLIS_ID =
            "com.github.jmmv.nudgytimer.lib.INTERVAL_END_MILLIS_ID";

    /** Forbid instantiation. */
    private IntentUtils() {}

    /**
     * Gets an interval from an intent previously put by putExtra.
     *
     * @param intent The intent from which to get the interval.
     *
     * @return The interval stored in the intent.
     *
     * @throws BadIntentException If the intent does not contain an interval or
     * the interval in the intent is invalid.  Should not happen if putInIntent
     * was used to store the interval in the intent.
     */
    public static Interval getIntervalExtra(final Intent intent)
            throws BadIntentException {
        if (!intent.hasExtra(START_MILLIS_ID)) {
            throw new BadIntentException("Intent is missing the startMillis " +
                                         "property of the Interval");
        }
        final long startMillis = intent.getLongExtra(START_MILLIS_ID, 0);

        if (!intent.hasExtra(END_MILLIS_ID)) {
            throw new BadIntentException("Intent is missing the endMillis " +
                                         "property of the Interval");
        }
        final long endMillis = intent.getLongExtra(END_MILLIS_ID, 0);

        try {
            return new Interval(startMillis, endMillis);
        } catch (final IllegalArgumentException e) {
            throw new BadIntentException("Backwards interval: " +
                                         e.getMessage());
        }
    }

    /**
     * Puts an interval in an intent.
     *
     * An intent can only hold a single interval put by this function.  You
     * should use getIntervalExtra to query the intent later on.
     *
     * @param intent The intent in which to put the interval.
     */
    public static void putExtra(final Intent intent, final Interval interval) {
        if (BuildConfig.DEBUG && (intent.hasExtra(START_MILLIS_ID) ||
                intent.hasExtra(END_MILLIS_ID))) {
            throw new AssertionError("Interval already put in intent; was " +
                                     "putExtra called earlier?");
        }
        intent.putExtra(START_MILLIS_ID, interval.getStartMillis());
        intent.putExtra(END_MILLIS_ID, interval.getEndMillis());
    }
}
