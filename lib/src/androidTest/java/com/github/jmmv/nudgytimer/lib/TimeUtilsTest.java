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

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class TimeUtilsTest extends AndroidTestCase {
    public void testForever() throws Exception {
        final Interval forever = TimeUtils.intervalForever();
        assertTrue(forever.contains(0));
        assertTrue(forever.contains(1000));
        assertTrue(forever.contains(123456789));
        assertTrue(forever.contains(Long.MAX_VALUE - 1));
        assertFalse(forever.contains(Long.MAX_VALUE));
    }

    public void testForDay() throws Exception {
        final Calendar cal = Calendar.getInstance();

        cal.set(2015, Calendar.NOVEMBER, 24, 12, 13, 30);
        final long timestamp = cal.getTimeInMillis();

        cal.set(2015, Calendar.NOVEMBER, 24, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long expectedBegin = cal.getTimeInMillis();

        cal.set(2015, Calendar.NOVEMBER, 24, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 999);
        final long expectedEnd = cal.getTimeInMillis();

        assertEquals(new Interval(expectedBegin, expectedEnd),
                     TimeUtils.intervalForDay(new DateTime(timestamp)));
    }

    public void testFormatDuration_SingularHardcoded() throws Exception {
        final Map<Duration, String> tests = new HashMap<>();
        tests.put(new Duration(1000), "This is a second");
        tests.put(new Duration(2000), "These are 2 seconds");
        tests.put(new Duration(1000 * 60), "This is a minute");
        tests.put(new Duration(2000 * 60), "These are 2 minutes");

        for (Map.Entry<Duration, String> test : tests.entrySet()) {
            final String string = TimeUtils.formatDuration(
                    test.getKey(), getContext(),
                    R.string.format_duration_one_second,
                    R.string.format_duration_seconds_plural,
                    R.string.format_duration_one_minute,
                    R.string.format_duration_minutes_plural);
            assertEquals(test.getValue(), string);
        }
    }

    public void testFormatDuration_SingularParameterized() throws Exception {
        final Map<Duration, String> tests = new HashMap<>();
        tests.put(new Duration(10), "These are 0 seconds");
        tests.put(new Duration(1000), "This is 1 second");
        tests.put(new Duration(2000), "These are 2 seconds");
        tests.put(new Duration(1000 * 60), "This is 1 minute");
        tests.put(new Duration(2000 * 60), "These are 2 minutes");
        tests.put(new Duration(1000 * 60 * 70), "These are 70 minutes");

        for (Map.Entry<Duration, String> test : tests.entrySet()) {
            final String string = TimeUtils.formatDuration(
                    test.getKey(), getContext(),
                    R.string.format_duration_seconds_singular,
                    R.string.format_duration_seconds_plural,
                    R.string.format_duration_minutes_singular,
                    R.string.format_duration_minutes_plural);
            assertEquals(test.getValue(), string);
        }
    }

    public void testFormatDurationMinutes_SingularHardcoded()
            throws Exception {
        final Map<Duration, String> tests = new HashMap<>();
        tests.put(new Duration(1000 * 60), "This is a minute");
        tests.put(new Duration(2000 * 60), "These are 2 minutes");

        for (Map.Entry<Duration, String> test : tests.entrySet()) {
            final String string = TimeUtils.formatDurationMinutes(
                    test.getKey(), getContext(),
                    R.string.format_duration_one_minute,
                    R.string.format_duration_minutes_plural);
            assertEquals(test.getValue(), string);
        }
    }

    public void testFormatDurationMinutes_SingularParameterized()
            throws Exception {
        final Map<Duration, String> tests = new HashMap<>();
        tests.put(new Duration(0), "These are 0 minutes");
        tests.put(new Duration(1), "These are 0 minutes");
        tests.put(new Duration(2000), "These are 0 minutes");
        tests.put(new Duration(1000 * 60), "This is 1 minute");
        tests.put(new Duration(2000 * 60), "These are 2 minutes");
        tests.put(new Duration(1000 * 60 * 70), "These are 70 minutes");

        for (Map.Entry<Duration, String> test : tests.entrySet()) {
            final String string = TimeUtils.formatDurationMinutes(
                    test.getKey(), getContext(),
                    R.string.format_duration_minutes_singular,
                    R.string.format_duration_minutes_plural);
            assertEquals(test.getValue(), string);
        }
    }
}
