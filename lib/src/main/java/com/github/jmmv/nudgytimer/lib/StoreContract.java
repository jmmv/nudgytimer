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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import org.joda.time.Interval;

import java.util.HashSet;
import java.util.Set;

/**
 * Database schema and interface to get and put high-level objects.
 */
final class StoreContract {
    /** Tag used for log messages. */
    private static final String TAG = "StoreContract";

    /**
     * Version of the database schema defined in this class.
     *
     * Any changes to the schema require the version to be bumped by 1.
     */
    public static final int SCHEMA_VERSION = 1;

    /** Prevent instantiation. */
    private StoreContract() {}

    /** Enumeration of the columns in the events table. */
    public static abstract class Events implements BaseColumns {
        public static final String TABLE_NAME = "events";
        public static final String COLUMN_NAME_START_MILLIS = "start_millis";
        public static final String COLUMN_NAME_END_MILLIS = "end_millis";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
    }

    /** Statement to create the events table. */
    private static final String CREATE_EVENTS_SQL =
        "CREATE TABLE " + Events.TABLE_NAME + " (" +
            Events._ID + " INTEGER PRIMARY KEY, " +
            Events.COLUMN_NAME_START_MILLIS + " INTEGER NOT NULL UNIQUE, " +
            Events.COLUMN_NAME_END_MILLIS + " INTEGER NOT NULL UNIQUE, " +
            Events.COLUMN_NAME_DESCRIPTION + " TEXT NOT NULL)";

    /** Statements to create the whole database. */
    public static final String[] CREATE_DB_SQL = { CREATE_EVENTS_SQL };

    /**
     * Puts an event in the events table.
     *
     * @param database Writable database in which to add the event.
     * @param event The event to add.
     *
     * @throws BadEventException If the event cannot be put because it would
     * overlap an existing event.
     */
    public static void putEvent(final SQLiteDatabase database,
                                final Event event)
            throws BadEventException, CorruptStoreError {
        final ContentValues values = new ContentValues();
        values.put(Events.COLUMN_NAME_START_MILLIS,
                   event.getStart().getMillis());
        values.put(Events.COLUMN_NAME_END_MILLIS,
                   event.getEnd().getMillis());
        values.put(Events.COLUMN_NAME_DESCRIPTION,
                   event.getDescription());

        database.beginTransaction();
        try {
            final Set<Event> overlapping = queryEvents(
                    database, event.getInterval(), 1L);
            if (!overlapping.isEmpty()) {
                throw new BadEventException(String.format(
                        "Cannot put new event %s because it would overlap " +
                        "other events in the database; one of them is: %s",
                        event.toString(),
                        overlapping.iterator().next().toString()));
            }

            final long rowId = database.insert(Events.TABLE_NAME, "null",
                                               values);
            if (rowId == -1) {
                Log.w(TAG, String.format("Failed to put event %s",
                                         event));
                throw new CorruptStoreError("Failed to put event");
            }
            Log.i(TAG, String.format("Put new event %s with row ID %d",
                                     event, rowId));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Instantiates an event with values queried from the database.
     *
     * @param startMillis Start time as obtained from the database.
     * @param endMillis End time as obtained from the database.
     * @param description Description as obtained from the database.
     * @param queryInterval Original interval used to fetch the event; used
     * to cut the event if the event's interval and the query interval
     * intersect.
     *
     * @return The parsed event.
     */
    private static Event parseEvent(final long startMillis,
                                    final long endMillis,
                                    final String description,
                                    final Interval queryInterval)
            throws CorruptStoreError {
        final Interval realInterval;
        try {
            realInterval = new Interval(startMillis, endMillis);
        } catch (final IllegalArgumentException e) {
            throw new CorruptStoreError("Bad event in database: %s" +
                                        e.getMessage());
        }

        final Interval effectiveRange = realInterval.overlap(queryInterval);
        if (BuildConfig.DEBUG && effectiveRange == null) {
            throw new AssertionError("Fetched event is outside of the " +
                                     "query interval");
        }

        try {
            return new Event(effectiveRange, description);
        } catch (final BadEventException e) {
            throw new CorruptStoreError("Bogus event in database");
        }
    }

    /**
     * Extracts events from a projection.
     *
     * @param cursor The cursor with the projection of the queries to extract.
     * @param queryRange The date used for the query; used to cut events that
     * were matched by the query but whose ends fall outside of the query
     * interval.
     *
     * @return The processed events.
     */
    private static Set<Event> parseEvents(
            final Cursor cursor, final Interval queryRange)
            throws CorruptStoreError {
        final int startMillisColumnId = cursor.getColumnIndex(
                Events.COLUMN_NAME_START_MILLIS);
        final int endMillisColumnId = cursor.getColumnIndex(
                Events.COLUMN_NAME_END_MILLIS);
        final int descriptionColumnId = cursor.getColumnIndex(
                Events.COLUMN_NAME_DESCRIPTION);

        final Set<Event> events = new HashSet<>();
        if (cursor.moveToFirst()) {
            do {
                final long startMillis = cursor.getLong(startMillisColumnId);
                final long endMillis = cursor.getLong(endMillisColumnId);
                final String description = cursor.getString(
                        descriptionColumnId);

                final Event event = parseEvent(
                        startMillis, endMillis, description, queryRange);
                if (BuildConfig.DEBUG && events.contains(event)) {
                    throw new AssertionError("Event should be unique in " +
                                             "the schema but got a duplicate");
                }
                events.add(event);
            } while (cursor.moveToNext());
        }
        return events;
    }

    /**
     * Queries events from the database.
     *
     * @param database The readable database to get the events from.
     * @param interval The time interval of the events to fetch.
     * @param limit Maximum number of events to return, or null for no limit.
     * If a limit is specified, the events are sorted in descending order by
     * their start time.
     *
     * @return The queried events.  If the query interval cuts through any
     * of the events at the borders of the interval, the returned events
     * have their start or end times adjusted accordingly to fall within the
     * interval.
     */
    public static Set<Event> queryEvents(
            final SQLiteDatabase database, final Interval interval,
            final Long limit) throws CorruptStoreError {
        final String startMillis = Long.toString(interval.getStartMillis());
        final String endMillis = Long.toString(interval.getEndMillis());
        try (Cursor cursor = database.rawQuery(
                "SELECT * FROM " + Events.TABLE_NAME +
                " WHERE (" + Events.COLUMN_NAME_START_MILLIS + " >= ? " +
                "AND " + Events.COLUMN_NAME_START_MILLIS + " < ?) " +
                "OR (" + Events.COLUMN_NAME_END_MILLIS + " >= ? " +
                "AND " + Events.COLUMN_NAME_END_MILLIS + " < ?) " +
                "ORDER BY " + Events.COLUMN_NAME_START_MILLIS + " DESC" +
                (limit == null ? "" : String.format(" LIMIT %d", limit)),
                new String[]{startMillis, endMillis, startMillis, endMillis})) {
            return parseEvents(cursor, interval);
        }
    }
}
