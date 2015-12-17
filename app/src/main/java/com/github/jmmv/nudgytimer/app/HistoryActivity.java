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

import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.github.jmmv.nudgytimer.lib.AggregateEvent;
import com.github.jmmv.nudgytimer.lib.BadIntentException;
import com.github.jmmv.nudgytimer.lib.IntentUtils;
import com.github.jmmv.nudgytimer.lib.TimeUtils;
import com.github.jmmv.nudgytimer.lib.Tracker;

import org.joda.time.Interval;

import java.util.SortedSet;

/**
 * Activity to display the history of a set of events.
 */
public final class HistoryActivity extends AppCompatActivity {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
            interval = IntentUtils.getIntervalExtra(getIntent());
        } catch (final BadIntentException e) {
            throw new AssertionError(e.getMessage());
        }

        populateEvents(interval);
    }

    /**
     * Populates the list of events with events that fall in the interval.
     *
     * @param interval The interval of the events to display.
     */
    private void populateEvents(final Interval interval) {
        final String[] matrix = { "_id", "event", "detail" };
        final String[] columns = { "event", "detail" };
        final int[] layouts = { android.R.id.text1, android.R.id.text2 };

        final MatrixCursor cursor = new MatrixCursor(matrix);

        final Tracker tracker =
                ((NudgyTimerApplication) getApplication()).getTracker();
        final SortedSet<AggregateEvent> aggregates =
                tracker.aggregateEventsInInterval(interval);
        int row = 0;
        // TODO(jmmv): Display aggregates backwards (i.e. largest first).
        for (final AggregateEvent aggregate : aggregates) {
            final String detail = TimeUtils.formatDuration(
                    aggregate.toDuration(), getBaseContext(),
                    R.string.total_seconds_singular,
                    R.string.total_seconds_plural,
                    R.string.total_minutes_singular,
                    R.string.total_minutes_plural);
            cursor.addRow(
                    new Object[]{row, aggregate.getDescription(), detail});
            row += 1;
        }

        final SimpleCursorAdapter data = new SimpleCursorAdapter(
                this, android.R.layout.simple_expandable_list_item_2, cursor,
                columns, layouts, Adapter.NO_SELECTION);
        final ListView eventsListView = (ListView)
                findViewById(R.id.eventsListView);
        eventsListView.setAdapter(data);
    }
}
