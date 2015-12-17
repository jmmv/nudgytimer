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

import android.content.Context;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.base.AbstractInstant;

import java.util.Calendar;

/**
 * Utilities to deal with time objects.
 */
public final class TimeUtils {
    /** Placeholder to replace in strings with a numeric duration. */
    private final static String duration_placeholder = "_DURATION_";

    /** Forbid instantiation. */
    private TimeUtils() {}

    /**
     * Returns an interval that matches any timestamp.
     */
    public static Interval intervalForever() {
        try {
            return new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
        } catch (final IllegalArgumentException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    /**
     * Returns a day-aligned interval that contains the given timestamp.
     *
     * @param timestamp Any timestamp within the day of interest for which to
     * compute the day boundaries.
     *
     * @return An interval that contains the timestamp.
     */
    public static Interval intervalForDay(final AbstractInstant timestamp) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp.toDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long start = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.add(Calendar.MILLISECOND, -1);
        final long end = cal.getTimeInMillis();

        try {
            return new Interval(start, end);
        } catch (final IllegalArgumentException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    /**
     * Formats a duration in minutes using a variety of localized strings.
     *
     * The strings must include a placeholder of the form _DURATION_, which will
     * be replaced by the corresponding duration in seconds or minutes.
     *
     * @param duration The duration to format.
     * @param context The application context from which to get the strings.
     * @param minutes_singular_id String ID for minutes formatting in plural.
     * May or may not include a _DURATION_ placeholder.
     * @param minutes_plural_id String ID for minutes formatting in plural.
     * Must include a _DURATION_ placeholder.
     *
     * @return A string with the duration formatted in it.
     */
    public static String formatDurationMinutes(final Duration duration,
                                               final Context context,
                                               final int minutes_singular_id,
                                               final int minutes_plural_id) {
        if (BuildConfig.DEBUG) {
            final String plural = context.getString(minutes_plural_id);
            if (!plural.contains(duration_placeholder)) {
                throw new AssertionError("Missing placeholder in plural string:"
                                         + plural);
            }
        }

        final long minutes = duration.getStandardMinutes();
        final int id;
        if (minutes == 1) {
            id = minutes_singular_id;
        } else {
            id = minutes_plural_id;
        }
        return context.getString(id).replaceAll(duration_placeholder,
                                                Long.toString(minutes));
    }

    /**
     * Formats a duration using a variety of localized strings.
     *
     * The strings must include a placeholder of the form _DURATION_, which will
     * be replaced by the corresponding duration in seconds or minutes.
     *
     * @param duration The duration to format.
     * @param context The application context from which to get the strings.
     * @param seconds_singular_id String ID for seconds formatting in singular.
     * May or may not include a _DURATION_ placeholder.
     * @param seconds_plural_id String ID for seconds formatting in plural.
     * Must include a _DURATION_ placeholder.
     * @param minutes_singular_id String ID for minutes formatting in plural.
     * May or may not include a _DURATION_ placeholder.
     * @param minutes_plural_id String ID for minutes formatting in plural.
     * Must include a _DURATION_ placeholder.
     *
     * @return A string with the duration formatted in it.
     */
    public static String formatDuration(final Duration duration,
                                        final Context context,
                                        final int seconds_singular_id,
                                        final int seconds_plural_id,
                                        final int minutes_singular_id,
                                        final int minutes_plural_id) {
        if (BuildConfig.DEBUG) {
            final String seconds_plural = context.getString(minutes_plural_id);
            if (!seconds_plural.contains(duration_placeholder)) {
                throw new AssertionError("Missing placeholder in plural string:"
                                         + seconds_plural);
            }
            final String minutes_plural = context.getString(minutes_plural_id);
            if (!minutes_plural.contains(duration_placeholder)) {
                throw new AssertionError("Missing placeholder in plural string:"
                                         + minutes_plural);
            }
        }

        final int id;
        final long quantity;

        final long minutes = duration.getStandardMinutes();
        if (minutes > 0) {
            if (minutes == 1) {
                id = minutes_singular_id;
            } else {
                id = minutes_plural_id;
            }
            quantity = minutes;
        } else {
            final long seconds = duration.getStandardSeconds();
            if (seconds == 1) {
                id = seconds_singular_id;
            } else {
                id = seconds_plural_id;
            }
            quantity = seconds;
        }

        return context.getString(id).replaceAll(duration_placeholder,
                                                Long.toString(quantity));
    }
}
