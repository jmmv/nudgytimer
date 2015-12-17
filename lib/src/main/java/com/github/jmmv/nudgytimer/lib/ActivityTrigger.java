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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Programs an alarm to start an activity after our configured poll time.
 *
 * This class provides methods to program, unprogram, and query the status of
 * a global alarm that fires a provided intent.  The operations in here are
 * intended to survive application restarts because the alarm is long-lived.
 */
public final class ActivityTrigger {
    /** Tag for log messages. */
    private static final String TAG = "ActivityTrigger";

    /** Android application context. */
    private final Context context;

    /** Settings accessor to fetch the latest value of the poll time. */
    private final Settings settings;

    /** A reference to the system's alarm manager. */
    private final AlarmManager alarmManager;

    /** An intent to start the desired activity when the alarm fires. */
    private final Intent intentFilter;

    // Fields to record stateful data for the class.
    final SharedPreferences privatePreferences;
    private static final String PREFS_ID =
            "com.github.jmmv.nudgytimer.lib.ActivityTrigger";
    static final String NEXT_FIRING_PREF_ID = "NEXT_FIRING";
    private static final String REQUEST_CODE_PREF_ID = "REQUEST_CODE_PREF_ID";
    private static final int PREF_UNSET = -1;

    /**
     * Constructs a new alarm.
     *
     * @param context The application context from which to retrieve the
     * alarm manager's instance.
     * @param settings The application-wide settings accessor.
     * @param activityClass The class of the activity to start.
     */
    public ActivityTrigger(final Context context,
                           final Settings settings,
                           final Class activityClass) {
        this.context = context;
        this.settings = settings;

        alarmManager = (AlarmManager)context.getSystemService(
                Context.ALARM_SERVICE);

        intentFilter = new Intent(context, activityClass);

        privatePreferences = context.getSharedPreferences(PREFS_ID,
                                                          Context.MODE_PRIVATE);
    }

    /**
     * Checks to see if the timer is programmed.
     *
     * We check two sources: the actual system timer and our own saved state.
     * Both should match, and we enforce this in debug conditions.  However,
     * in release mode, we favor the value of the system timer.
     *
     * @return True if the timer is programmed; false otherwise.
     */
    public boolean isProgrammed() {
        final long nextFiring = privatePreferences.getLong(
                NEXT_FIRING_PREF_ID, PREF_UNSET);
        final int requestCode = privatePreferences.getInt(
                REQUEST_CODE_PREF_ID, PREF_UNSET);
        final PendingIntent pendingIntent;
        if (requestCode != PREF_UNSET) {
            pendingIntent = PendingIntent.getActivity(
                    context, requestCode, intentFilter,
                    PendingIntent.FLAG_NO_CREATE);
            Log.i(TAG, String.format("Pending intents for requestCode=%d: %b",
                                     requestCode, pendingIntent != null));
        } else {
            pendingIntent = null;
        }

        final boolean predicted = (nextFiring != PREF_UNSET);
        final boolean actual = (pendingIntent != null);
        Log.i(TAG, String.format("Guesses: predicted=%b, actual=%b",
                                 predicted, actual));

        if (predicted) {
            if (nextFiring < System.currentTimeMillis()) {
                // Our knowledge is stale so we have to assume that the alarm
                // already fired and that it is not programmed any longer.
                return false;
            } else if (!actual) {
                Log.w(TAG, "The timer is not programmed but our saved state " +
                      "does not agree; assuming not programmed");
                if (BuildConfig.DEBUG) {
                    throw new AssertionError();
                }
                return false;
            }
        } else if (actual) {
            Log.w(TAG, "The timer is programmed but our saved state does not " +
                  "agree; assuming programmed");
            if (BuildConfig.DEBUG) {
                throw new AssertionError();
            }
            return true;
        }

        return actual;
    }

    /**
     * Gets the predicted next activation of the timer.
     *
     * This timing is approximate and represents the time we programmed this
     * for.  The actual activation will happen after the time returned here.
     *
     * @return A timestamp in wall clock time or null if either the timer
     * already fired or we know the timer is programmed but we do not know
     * when it will fire.
     */
    public DateTime getNextFiring() {
        final long nextFiring = privatePreferences.getLong(
                NEXT_FIRING_PREF_ID, PREF_UNSET);
        if (nextFiring == PREF_UNSET ||
                nextFiring < System.currentTimeMillis()) {
            return null;
        } else {
            return new DateTime(nextFiring);
        }
    }

    /**
     * Programs the alarm to fire at the next interval desired by the user.
     */
    public void programOneShot() {
        if (BuildConfig.DEBUG && isProgrammed()) {
            throw new AssertionError("Cannot call when timer is already set");
        }

        final DateTime now = DateTime.now();
        final Interval interval = new Interval(
                now, now.plus(settings.getPollPeriod()));

        final int previousRequestCode = privatePreferences.getInt(
                REQUEST_CODE_PREF_ID, PREF_UNSET);
        final int requestCode = (int)interval.getStartMillis();
        if (BuildConfig.DEBUG && requestCode == previousRequestCode) {
            throw new AssertionError("Repeated requestCode; cannot tell " +
                                     "them apart");
        }

        final Intent intent = (Intent)intentFilter.clone();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentUtils.putExtra(intent, interval);
        final PendingIntent alarmIntent = PendingIntent.getActivity(
                context, requestCode, intent, 0);

        final SharedPreferences.Editor editor = privatePreferences.edit();
        editor.putLong(NEXT_FIRING_PREF_ID, interval.getEndMillis());
        editor.putInt(REQUEST_CODE_PREF_ID, requestCode);
        editor.apply();

        Log.i(TAG, String.format(
                "Programming alarm to trigger on %s; requestCode=%d",
                interval.getEnd().toString(), requestCode));
        alarmManager.setExact(AlarmManager.RTC, interval.getEndMillis(),
                              alarmIntent);

        if (!isProgrammed()) {
            Log.w(TAG, "Programmed alarm seems to have fired before leaving " +
                  "programOneShot!");
        }
    }

    /**
     * Clears the already-programmed alarm.
     *
     * This ensures that both the system timer and our own knowledge of it
     * are cleared, regardless of any inconsistencies between the two.
     */
    public void stop() {
        if (BuildConfig.DEBUG && !isProgrammed()) {
            throw new AssertionError("Cannot call when timer is not set");
        }

        wipeState();

        if (BuildConfig.DEBUG && isProgrammed()) {
            throw new AssertionError();
        }
    }

    /**
     * Wipes the timer's state without first checking if it is programmed.
     */
    void wipeState() {
        Log.i(TAG, "Clearing trigger state");

        final int requestCode = privatePreferences.getInt(
                REQUEST_CODE_PREF_ID, PREF_UNSET);
        if (requestCode != PREF_UNSET) {
            Log.i(TAG, String.format(
                    "Finding pending intents for requestCode=%d", requestCode));
            final PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, requestCode, intentFilter,
                    PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent == null) {
                Log.i(TAG, "No pending intent found");
            } else {
                pendingIntent.cancel();
                alarmManager.cancel(pendingIntent);
                Log.i(TAG, "Cancelled pending intent and alarm");
            }
        } else {
            Log.i(TAG, "No requestCode recorded; not clearing alarms");
        }

        final SharedPreferences.Editor editor = privatePreferences.edit();
        editor.remove(NEXT_FIRING_PREF_ID);
        editor.remove(REQUEST_CODE_PREF_ID);
        editor.apply();
    }
}
