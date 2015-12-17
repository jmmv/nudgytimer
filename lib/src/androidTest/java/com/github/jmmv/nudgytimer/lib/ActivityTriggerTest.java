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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

// TODO(jmmv): These tests are inherently flaky because they play with time, and
// they are also slow to run due to the delays in them.  The only way I can see
// now to make this better is to mock the time calls as well... but that sounds
// nasty for different reasons.
public final class ActivityTriggerTest extends InstrumentationTestCase {
    /** Tag for log messages. */
    private static final String TAG = "ActivityTriggerTest";

    /** Target context for the test. */
    private Instrumentation.ActivityMonitor activityMonitor;

    /** Instance of the waited-for activity for cleanup purposes. */
    private List<Activity> startedActivities;

    /** Instance of the trigger created by newTrigger for cleanup purposes. */
    private ActivityTrigger activityTriggerInstance;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        activityMonitor = getInstrumentation().addMonitor(
                TestActivity.class.getName(), null, false);
        startedActivities = new ArrayList<>();

        activityTriggerInstance = null;
    }

    @Override
    protected void tearDown() throws Exception {
        if (activityTriggerInstance != null) {
            Log.i(TAG, "Wiping ActivityTrigger state on tearDown");
            activityTriggerInstance.wipeState();
        }

        for (final Activity activity : startedActivities) {
            Log.i(TAG, "Finishing started activity on tearDown");
            destroyActivity(activity);
        }

        getInstrumentation().removeMonitor(activityMonitor);

        super.tearDown();
    }

    /**
     * Finishes an activity and waits for its termination.
     */
    private void destroyActivity(final Activity activity) throws Exception {
        activity.finish();
        while (!activity.isDestroyed()) {
            Thread.sleep(10);
        }
    }

    /**
     * Checks if a value is within a range.
     *
     * @param value The value to be tested.
     * @param min Minimum acceptable value.
     * @param max Maximum acceptable value.
     */
    private void assertInRange(final long value,
                               final long min, final long max) {
        if (value < min || value > max) {
            fail(String.format("%d is not within [%d,%d]", value, min, max));
        }
    }

    /**
     * Waits for an activity to start.
     *
     * Use this instead of querying the ActivityMonitor directly because we
     * want to keep track of the started activities for cleanup purposes.
     *
     * @param timeoutMillis Timeout for the wait.
     * @param userMonitor The monitor to use for the wait.  If null, use the
     * default monitor for the test.
     *
     * @return The started activity, or null if no activity started.
     */
    private Activity waitForActivity(
            final long timeoutMillis,
            final Instrumentation.ActivityMonitor userMonitor) {
        Log.i(TAG, String.format("Waiting for activity for %d ms",
                                 timeoutMillis));

        final Instrumentation.ActivityMonitor monitor =
                userMonitor == null ? activityMonitor : userMonitor;
        final Activity activity =
                monitor.waitForActivityWithTimeout(timeoutMillis);
        Log.i(TAG, String.format("Waited for activity for %d ms; found: %b",
                                 timeoutMillis, activity != null));
        if (activity != null) {
            startedActivities.add(activity);
        }
        return activity;
    }

    /**
     * Waits for an activity to start within an specified time range.
     *
     * Use this instead of querying the ActivityMonitor directly because we
     * want to keep track of the started activities for cleanup purposes.
     *
     * @param minOkMillis Minimum acceptable delay for the wait to complete.
     * @param maxOkMillis Maximum acceptable delay for the wait to complete.
     * @param monitor The monitor to use for the wait.
     *
     * @return The started activity.
     */
    private Activity waitForActivityAndCheck(
            final long minOkMillis,
            final long maxOkMillis,
            final Instrumentation.ActivityMonitor monitor) {
        final long before = System.currentTimeMillis();
        final Activity activity = waitForActivity(maxOkMillis * 2, monitor);
        final long delay = System.currentTimeMillis() - before;

        assertNotNull(activity);
        assertInRange(delay, minOkMillis, maxOkMillis);

        return activity;
    }

    /**
     * Version of waitForActivityAndCheck using the default monitor.
     */
    private Activity waitForActivityAndCheck(final long minOkMillis,
                                             final long maxOkMillis) {
        return waitForActivityAndCheck(minOkMillis, maxOkMillis,
                                       activityMonitor);
    }

    /**
     * Creates a new test settings.
     *
     * @param pollPeriodMillis Delay after which to fire the timer.
     *
     * @return The application settings.
     */
    private FakeSettings newSettings(final long pollPeriodMillis) {
        final FakeSettings settings = new FakeSettings();
        settings.setPollPeriod(new Duration(pollPeriodMillis));
        return settings;
    }

    /**
     * Creates a new activity trigger instance.
     *
     * Use this instead of instantiating a ActivityTrigger directly, because we
     * want to keep track of the started instances for cleanup purposes.  Can
     * only be called once within each test case.
     *
     * @param settings The settings to use for the trigger.
     *
     * @return A new ActivityTrigger instance.
     */
    private ActivityTrigger newTrigger(final Settings settings) {
        assertNull(activityTriggerInstance);
        final ActivityTrigger activityTrigger = new ActivityTrigger(
                getInstrumentation().getTargetContext(), settings,
                TestActivity.class);
        activityTriggerInstance = activityTrigger;
        Log.i(TAG, "Instantiated new activityTrigger");
        return activityTrigger;
    }

    public void testIsProgrammed() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(10000));
        assertFalse(activityTrigger.isProgrammed());
        activityTrigger.programOneShot();
        assertTrue(activityTrigger.isProgrammed());
        activityTrigger.stop();
        assertFalse(activityTrigger.isProgrammed());
    }

    public void testGetNextFiring() throws Exception {
        final DateTime now = DateTime.now();
        final ActivityTrigger activityTrigger = newTrigger(newSettings(5000));
        activityTrigger.programOneShot();

        final DateTime nextFiring = activityTrigger.getNextFiring();
        assertNotNull(nextFiring);
        assertInRange(nextFiring.getMillis(),
                      now.getMillis(), now.getMillis() + 10000);
    }

    public void testGetNextFiring_InThePast() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(100));
        activityTrigger.programOneShot();
        Thread.sleep(1000);
        assertNull(activityTrigger.getNextFiring());
    }

    public void testGetNextFiring_Unknown() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(5000));
        activityTrigger.programOneShot();

        final SharedPreferences.Editor editor =
                activityTrigger.privatePreferences.edit();
        editor.remove(ActivityTrigger.NEXT_FIRING_PREF_ID);
        editor.apply();

        assertNull(activityTrigger.getNextFiring());
    }

    public void testOneShot_Single() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(5000));
        activityTrigger.programOneShot();
        waitForActivityAndCheck(3000, 8000);
    }

    public void testOneShot_Repeat() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(5000));
        activityTrigger.programOneShot();
        final Activity activity = waitForActivityAndCheck(2000, 7000);
        destroyActivity(activity);

        final Instrumentation.ActivityMonitor activityMonitor2 =
                getInstrumentation().addMonitor(
                        TestActivity.class.getName(), null, false);
        try {
            activityTrigger.programOneShot();
            waitForActivityAndCheck(2000, 7000, activityMonitor2);
        } finally {
            getInstrumentation().removeMonitor(activityMonitor2);
        }
    }

    public void testStop() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(2000));
        activityTrigger.programOneShot();
        activityTrigger.stop();
        assertNull(waitForActivity(5000, null));
    }

    public void testHonorSettingsChanges() throws Exception {
        final FakeSettings settings = newSettings(100000);
        final ActivityTrigger activityTrigger = newTrigger(settings);
        settings.setPollPeriod(new Duration(5000));
        activityTrigger.programOneShot();
        waitForActivityAndCheck(2000, 7000);
    }

    public void testIntentContents() throws Exception {
        final ActivityTrigger activityTrigger = newTrigger(newSettings(5000));
        activityTrigger.programOneShot();
        final DateTime afterProgramming = DateTime.now();

        final Activity activity = waitForActivityAndCheck(3000, 8000);
        assertNotNull(activity);
        final DateTime afterWait = DateTime.now();

        final Interval interval = IntentUtils.getIntervalExtra(
                activity.getIntent());
        assertTrue(interval.getStart().isBefore(afterProgramming) ||
                   interval.getStart().equals(afterProgramming));
        assertTrue(interval.getEnd().isAfter(afterProgramming) ||
                   interval.getEnd().equals(afterProgramming));
        assertTrue(interval.getEnd().isBefore(afterWait) ||
                   interval.getEnd().equals(afterWait));
    }
}
