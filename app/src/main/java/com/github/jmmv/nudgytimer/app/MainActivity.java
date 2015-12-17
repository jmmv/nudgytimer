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
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.github.jmmv.nudgytimer.lib.BaseEvent;
import com.github.jmmv.nudgytimer.lib.IntentUtils;
import com.github.jmmv.nudgytimer.lib.TimeUtils;
import com.github.jmmv.nudgytimer.lib.Tracker;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Application's entry activity.
 */
public final class MainActivity extends AppCompatActivity {
    /** Coordinator for the application's ActivityTrigger and the UI views. */
    private Countdown countdown;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        countdown = new Countdown(
                ((NudgyTimerApplication) getApplication()).getActivityTrigger(),
                (Switch) findViewById(R.id.trackSwitch),
                (TextView) findViewById(R.id.countdownTextView),
                ((NudgyTimerApplication) getApplication()).getSettings(),
                getBaseContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();

        populateSummary((ListView) findViewById(R.id.summaryListView));
        countdown.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();

        countdown.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates a summary description for an event.
     *
     * @param event The event to display.
     *
     * @return A summarized description of the event in textual form.
     */
    private String formatEvent(final BaseEvent event) {
        if (event == null) {
            return getBaseContext().getString(R.string.event_summary_na);
        } else {
            final String string = TimeUtils.formatDuration(
                    event.toDuration(), getBaseContext(),
                    R.string.event_summary_seconds_singular,
                    R.string.event_summary_seconds_plural,
                    R.string.event_summary_minutes_singular,
                    R.string.event_summary_minutes_plural);
            return string.replaceAll("_DESCRIPTION_", event.getDescription());
        }
    }

    /**
     * Populates a list with a summary of the events in the tracker.
     *
     * @param summary The widget to populate.
     */
    private void populateSummary(final ListView summary) {
        final String[] matrix  = { "_id", "name", "value" };
        final String[] columns = { "name", "value" };
        final int[] layouts = { android.R.id.text1, android.R.id.text2 };

        final MatrixCursor cursor = new MatrixCursor(matrix);

        final Tracker tracker =
                ((NudgyTimerApplication) getApplication()).getTracker();

        final Interval todayRange = TimeUtils.intervalForDay(DateTime.now());
        final Interval foreverRange = TimeUtils.intervalForever();

        cursor.addRow(new Object[]{
                0, getString(R.string.last_activity_label),
                formatEvent(tracker.mostRecentEvent())});
        cursor.addRow(new Object[]{
                1, getString(R.string.top_activity_today_label),
                formatEvent(tracker.topEventInInterval(todayRange))});
        cursor.addRow(new Object[]{
                2, getString(R.string.top_activity_all_label),
                formatEvent(tracker.topEventInInterval(foreverRange))});

        final SimpleCursorAdapter data = new SimpleCursorAdapter(
                this, android.R.layout.simple_expandable_list_item_2, cursor,
                columns, layouts, Adapter.NO_SELECTION);
        summary.setAdapter(data);

        summary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent,
                                    final View view, final int position,
                                    final long id) {
                //noinspection StatementWithEmptyBody
                if (position == 0) {
                    // Do nothing.
                } else if (position == 1) {
                    Intent intent = new Intent(MainActivity.this,
                                               HistoryActivity.class);
                    IntentUtils.putExtra(intent, todayRange);
                    startActivity(intent);
                } else if (position == 2) {
                    Intent intent = new Intent(MainActivity.this,
                                               HistoryActivity.class);
                    IntentUtils.putExtra(intent, foreverRange);
                    startActivity(intent);
                } else {
                    if (BuildConfig.DEBUG) {
                        throw new AssertionError();
                    }
                }
            }
        });
    }
}
