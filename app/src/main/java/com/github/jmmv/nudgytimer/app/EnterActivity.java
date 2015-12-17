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

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.jmmv.nudgytimer.lib.ActivityTrigger;
import com.github.jmmv.nudgytimer.lib.BadEventException;
import com.github.jmmv.nudgytimer.lib.BadIntentException;
import com.github.jmmv.nudgytimer.lib.Event;
import com.github.jmmv.nudgytimer.lib.IntentUtils;
import com.github.jmmv.nudgytimer.lib.TimeUtils;
import com.github.jmmv.nudgytimer.lib.Tracker;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Activity to record a new event.
 *
 * This activity is triggered when the query period expires.  Its
 * purpose is to make the user enter a summary of what they have been
 * doing since the previous prompt.
 *
 * The intent triggering this activity needs to include the time interval
 * from when the tracking started to the time the activity fired.
 */
public final class EnterActivity extends AppCompatActivity {
    /** Tag for logging purposes. */
    private static final String TAG = "EnterActivity";

    /** Vibration pattern for notifications. */
    private static final long[] vibrationPattern = {0, 200, 100, 200, 100, 200};

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();

        final Interval interval;
        try {
            final Intent intent = getIntent();
            interval = IntentUtils.getIntervalExtra(intent);
            Log.i(TAG, String.format("Interval in intent is: %s",
                                     interval.toString()));
        } catch (final BadIntentException e) {
            // TODO(jmmv): Should display a dialog to notify the user of the
            // invalid intent instead of just ignoring it.
            Log.i(TAG, String.format("Ignoring invalid interval in intent: %s",
                                     e.toString()));
            finish();
            return;
        }

        populateWidgets(interval);
        programActions(interval);
        notifyUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        final ActivityTrigger activityTrigger =
                ((NudgyTimerApplication) getApplication()).getActivityTrigger();
        if (!activityTrigger.isProgrammed()) {
            activityTrigger.programOneShot();
        }

        super.onDestroy();
    }

    /**
     * Sets up the view contents.
     *
     * @param interval Interval representing the time since the last event
     * query and when this activity was triggered.
     */
    private void populateWidgets(final Interval interval) {
        final TextView view = (TextView) findViewById(
                R.id.activityPromptTextView);
        view.setText(TimeUtils.formatDurationMinutes(
                interval.toDuration(), getBaseContext(),
                R.string.ask_activity_minutes_singular,
                R.string.ask_activity_minutes_plural));

        final EditText text = (EditText)
                findViewById(R.id.descriptionEditText);
        final Tracker tracker =
                ((NudgyTimerApplication) getApplication()).getTracker();
        final Event event = tracker.mostRecentEvent();
        final String description = event == null ? "" : event.getDescription();
        text.setText(description);
        text.setSelection(0, description.length());
    }

    /**
     * Programs the actions of the view.
     *
     * @param interval Interval representing the time since the last event
     * query and when this activity was triggered.
     */
    private void programActions(final Interval interval) {
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final EditText text = (EditText) findViewById(
                        R.id.descriptionEditText);
                final Tracker tracker =
                        ((NudgyTimerApplication) getApplication()).getTracker();
                try {
                    // TODO(jmmv): I think recording the event using the
                    // previous start and the current time is the way to go,
                    // in case the user is so focused on his current activity
                    // that he doesn't want to enter the details now... but then
                    // this means that what we display in populateWidgets() is
                    // inconsistent.  Figure out what to do.
                    tracker.addEvent(interval.getStart(), DateTime.now(),
                                     text.getText().toString());
                } catch (final BadEventException e) {
                    throw new AssertionError(e.getMessage());
                }
                finish();
            }
        });

        final Button discardButton = (Button) findViewById(R.id.discardButton);
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                finish();
            }
        });
    }

    /**
     * Notifies the user of the need to record a new event.
     */
    private void notifyUser() {
        // TODO(jmmv): Add configuration options for notifications and add
        // support for audible notifications.
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(vibrationPattern, -1);
    }
}
