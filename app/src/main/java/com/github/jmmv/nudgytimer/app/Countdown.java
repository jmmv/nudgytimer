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
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.github.jmmv.nudgytimer.lib.ActivityTrigger;
import com.github.jmmv.nudgytimer.lib.Settings;
import com.github.jmmv.nudgytimer.lib.TimeUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;

/**
 * Logic to coordinate the ActivityTrigger, its switch, and its display.
 *
 * The parent activity is responsible for invoking onResume() and onPause()
 * according to its own lifecycle.
 *
 * TODO(jmmv): I suspect this whole thing would better be represented as a
 * custom view.
 */
class Countdown {
    /** Tag for log messages. */
    private static final String TAG = "Countdown";

    /** The activity trigger that backs the timer for this countdown. */
    private final ActivityTrigger activityTrigger;

    /** The knob to control the countdown. */
    private final Switch trackSwitch;

    /** The text view that displays the countdown. */
    private final TextView textView;

    /** Settings accessor to fetch the latest value of the poll time. */
    private final Settings settings;

    /** Android application context. */
    private final Context context;

    // Fields to record stateful data for the class.
    private final SharedPreferences privatePreferences;
    private static final String PREFS_ID =
            "com.github.jmmv.nudgytimer.app.Countdown";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String ENABLED_PREF_ID = "ENABLED_PREF_ID";

    /** The ticking timer for the display. */
    private CountDownTimer timer;

    /**
     * Creates a countdown and wires it to a view.
     *
     * @param activityTrigger The activity trigger that backs this countdown.
     * @param trackSwitch The knob that controls this countdown.
     * @param textView The text view in which to display the countdown.
     * @param settings Settings accessor to fetch the poll time.
     * @param context The application context.
     */
    public Countdown(final ActivityTrigger activityTrigger,
                     final Switch trackSwitch,
                     final TextView textView,
                     final Settings settings,
                     final Context context) {
        this.activityTrigger = activityTrigger;
        this.trackSwitch = trackSwitch;
        this.textView = textView;
        this.settings = settings;
        this.context = context;

        this.privatePreferences = context.getSharedPreferences(
                PREFS_ID, Context.MODE_PRIVATE);

        trackSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSwitch(trackSwitch.isChecked());
            }
        });
    }

    /**
     * Reconfigures the countdown from persistent state at resume time.
     *
     * The parent activity must call this method when it receives its own
     * onResume() notification.
     */
    public void onResume() {
        trackSwitch.setText(TimeUtils.formatDurationMinutes(
                settings.getPollPeriod(), context,
                R.string.track_switch_label_singular,
                R.string.track_switch_label_plural));
        trackSwitch.setChecked(
                privatePreferences.getBoolean(ENABLED_PREF_ID, false));
        handleSwitch(trackSwitch.isChecked());
    }

    /**
     * Prepares the countdown to stop, persisting its state.
     *
     * The parent activity must call this method when it receives its own
     * onPause() notification.
     */
    public void onPause() {
        SharedPreferences.Editor editor = privatePreferences.edit();
        editor.putBoolean(ENABLED_PREF_ID, trackSwitch.isChecked());
        editor.apply();
    }

    /**
     * Displays the given duration in the countdown text label.
     *
     * @param duration The duration to display.
     */
    private void setDisplay(final Duration duration) {
        final long minutes;
        final long seconds;
        if (duration == null) {
            minutes = 0;
            seconds = 0;
        } else {
            minutes = duration.getStandardMinutes();
            seconds = duration.getStandardSeconds() %
                    DateTimeConstants.SECONDS_PER_MINUTE;
        }
        textView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    /**
     * Synchronizes the display with the activity trigger.
     *
     * This queries the time until the next timer activation from persistent
     * state and sets the countdown timer accordingly.
     */
    private void start() {
        final DateTime nextFiring = activityTrigger.getNextFiring();
        if (nextFiring == null) {
            // This is unlikely but could happen.  Consider the case where we
            // have successfully programmed the system timer but then the app
            // gets destroyed and does not come back until a much later time.
            Log.w(TAG, "Not starting UI countdown because the system timer " +
                  "already expired");
            return;
        }

        final long remainder = (nextFiring.getMillis() -
                                DateTime.now().getMillis());
        setDisplay(new Duration(remainder));

        Log.i(TAG, String.format("Starting UI countdown for %dms", remainder));
        timer = new CountDownTimer(remainder,
                                   DateTimeConstants.MILLIS_PER_SECOND) {
            @Override
            public void onTick(final long msUntilFinished) {
                setDisplay(new Duration(msUntilFinished));
            }

            @Override
            public void onFinish() {
                stop();
            }
        };
        timer.start();
    }

    /**
     * Stops the countdown and clears the display.
     */
    private void stop() {
        setDisplay(null);

        timer.cancel();
        timer = null;
    }

    /**
     * Enables or disables the countdown based on the value of the track switch.
     *
     * @param enabled The current value of the track switch.
     */
    private void handleSwitch(final boolean enabled) {
        if (enabled) {
            if (!activityTrigger.isProgrammed()) {
                activityTrigger.programOneShot();
            }
            if (timer == null) {
                start();
            }
        } else {
            if (timer != null) {
                stop();
            }
            activityTrigger.stop();
        }
        textView.setEnabled(enabled);
    }
}
