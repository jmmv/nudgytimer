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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

public class SqliteTrackerTest extends AndroidTestCase {
    /**
     * The database used to back the test tracker.
     *
     * We keep this as a class attribute so that tests can directly inspect the
     * state of the database if they need to do so.
     */
    private SQLiteDatabase database = null;

    /**
     * The tracker for testing, backed by an in-memory SQLite database.
     *
     * This tracker is recreated for each test case so it can be assumed that
     * it is empty at the beginning of each test.
     */
    private Tracker tracker = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final StoreHelper helper = StoreHelper.newInMemory(getContext());
        database = helper.getWritableDatabase();
        tracker = new SqliteTracker(database);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        tracker = null;
        database = null;
    }

    /**
     * Counts the number of events in the temporary SQLite database.
     */
    private int countEvents() {
        try (Cursor cursor = database.rawQuery(
                "SELECT COUNT(" + StoreContract.Events._ID + ") AS count " +
                        "FROM " + StoreContract.Events.TABLE_NAME, null)) {
            assertTrue(cursor.moveToNext());
            final int count = cursor.getInt(cursor.getColumnIndex("count"));
            assertFalse(cursor.moveToNext());
            return count;
        }
    }

    public void testAddEvent_One() throws Exception {
        assertEquals(0, countEvents());
        tracker.addEvent(new DateTime(1), new DateTime(2), "One element");
        assertEquals(1, countEvents());
    }

    public void testAddEvent_Several() throws Exception {
        assertEquals(0, countEvents());
        tracker.addEvent(new DateTime(10), new DateTime(15), "Element");
        tracker.addEvent(new DateTime(20), new DateTime(25), "Element");
        tracker.addEvent(new DateTime(30), new DateTime(35), "Element");
        assertEquals(3, countEvents());
    }

    public void testAddEvent_Duplicate() throws Exception {
        tracker.addEvent(new DateTime(10), new DateTime(15), "First");
        tracker.addEvent(new DateTime(20), new DateTime(25), "Second");
        tracker.addEvent(new DateTime(30), new DateTime(35), "Second");
        final int count = countEvents();
        try {
            tracker.addEvent(new DateTime(20), new DateTime(25), "Second bis");
            fail("Should have thrown BadEventException");
        } catch (final BadEventException e) {
            assertTrue(e.toString().matches(".*would overlap.*"));
        }
        assertEquals(count, countEvents());
    }

    public void testAddEvent_Overlapping() throws Exception {
        tracker.addEvent(new DateTime(10), new DateTime(15), "First");
        tracker.addEvent(new DateTime(20), new DateTime(25), "Second");
        tracker.addEvent(new DateTime(30), new DateTime(35), "Second");
        final int count = countEvents();
        try {
            tracker.addEvent(new DateTime(23), new DateTime(28), "Second bis");
            fail("Should have thrown BadEventException");
        } catch (final BadEventException e) {
            assertTrue(e.toString().matches(".*overlap other events.*"));
        }
        assertEquals(count, countEvents());
    }

    public void testEventsInInterval_NoEventsInTracker() throws Exception {
        final Set<Event> events =
                tracker.eventsInInterval(TimeUtils.intervalForever());
        assertTrue(events.isEmpty());
    }

    public void testEventsInInterval_NoEventsInInterval() throws Exception {
        tracker.addEvent(new DateTime(10), new DateTime(20), "Outside");
        tracker.addEvent(new DateTime(30), new DateTime(40), "Outside");
        tracker.addEvent(new DateTime(80), new DateTime(90), "Outside");

        final Set<Event> events =
                tracker.eventsInInterval(new Interval(41, 79));
        assertTrue(events.isEmpty());
    }

    public void testEventsInInterval_SomeEventsInInterval() throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(9), "Outside");
        tracker.addEvent(new DateTime(10), new DateTime(19), "Outside");
        tracker.addEvent(new DateTime(20), new DateTime(29), "Inside 1");
        tracker.addEvent(new DateTime(40), new DateTime(49), "Inside 3");
        tracker.addEvent(new DateTime(50), new DateTime(59), "Inside 2");
        tracker.addEvent(new DateTime(90), new DateTime(9999), "Outside");

        final Set<Event> expected = new HashSet<>();
        expected.add(new Event(new Interval(20, 29), "Inside 1"));
        expected.add(new Event(new Interval(50, 59), "Inside 2"));
        expected.add(new Event(new Interval(40, 49), "Inside 3"));

        assertEquals(expected,
                     tracker.eventsInInterval(new Interval(20, 80)));
    }

    public void testEventsInInterval_CutExteriors() throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(9), "Outside");
        tracker.addEvent(new DateTime(10), new DateTime(19),
                         "Partially inside");
        tracker.addEvent(new DateTime(20), new DateTime(29), "Inside");
        tracker.addEvent(new DateTime(30), new DateTime(39),
                         "Partially inside");
        tracker.addEvent(new DateTime(40), new DateTime(49), "Outside");

        final Set<Event> expected = new HashSet<>();
        expected.add(new Event(new Interval(17, 19), "Partially inside"));
        expected.add(new Event(new Interval(20, 29), "Inside"));
        expected.add(new Event(new Interval(30, 36), "Partially inside"));

        assertEquals(expected,
                     tracker.eventsInInterval(new Interval(17, 36)));
    }

    public void testAggregateEventsInInterval_NoEventsInTracker()
            throws Exception {
        final SortedSet<AggregateEvent> aggregates =
                tracker.aggregateEventsInInterval(TimeUtils.intervalForever());
        assertTrue(aggregates.isEmpty());
    }

    public void testAggregateEventsInInterval_NoEventsInInterval()
            throws Exception {
        tracker.addEvent(new DateTime(10), new DateTime(20), "Outside");
        tracker.addEvent(new DateTime(30), new DateTime(40), "Outside");
        tracker.addEvent(new DateTime(80), new DateTime(90), "Outside");

        final SortedSet<AggregateEvent> aggregates =
                tracker.aggregateEventsInInterval(new Interval(41, 79));
        assertTrue(aggregates.isEmpty());
    }

    public void testAggregateEventsInInterval_SomeEventsInIntervalAndCut()
            throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(9), "Outside");
        tracker.addEvent(new DateTime(10), new DateTime(19),
                         "Partially inside");
        tracker.addEvent(new DateTime(20), new DateTime(29), "Inside");
        tracker.addEvent(new DateTime(30), new DateTime(39),
                         "Partially inside");
        tracker.addEvent(new DateTime(40), new DateTime(49), "Outside");

        final SortedSet<AggregateEvent> aggregates =
                tracker.aggregateEventsInInterval(new Interval(17, 36));
        assertEquals(2, aggregates.size());
        assertEquals("Partially inside", aggregates.first().getDescription());
        assertEquals(8, aggregates.first().toDuration().getMillis());
        assertEquals("Inside", aggregates.last().getDescription());
        assertEquals(9, aggregates.last().toDuration().getMillis());
    }

    public void testTopEventInInterval_NoEventsInTracker()
            throws Exception {
        final AggregateEvent aggregate =
                tracker.topEventInInterval(new Interval(0, 2000));
        assertNull(aggregate);
    }

    public void testTopEventInInterval_SomeEventsInTracker()
            throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(1), "One");
        tracker.addEvent(new DateTime(2), new DateTime(3), "One");
        tracker.addEvent(new DateTime(4), new DateTime(5), "Two");
        tracker.addEvent(new DateTime(6), new DateTime(7), "Two");
        tracker.addEvent(new DateTime(8), new DateTime(9), "One");
        tracker.addEvent(new DateTime(10), new DateTime(11), "Three");
        tracker.addEvent(new DateTime(12), new DateTime(13), "Two");
        tracker.addEvent(new DateTime(14), new DateTime(15), "Two");
        tracker.addEvent(new DateTime(16), new DateTime(17), "Two");
        tracker.addEvent(new DateTime(18), new DateTime(19), "Three");

        final AggregateEvent aggregate =
                tracker.topEventInInterval(new Interval(0, 100));
        assertEquals("Two", aggregate.getDescription());
        assertEquals(5, aggregate.toDuration().getMillis());
    }

    public void testTopEventInInterval_CutExteriors()
            throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(99), "A");
        tracker.addEvent(new DateTime(100), new DateTime(119), "B");
        tracker.addEvent(new DateTime(120), new DateTime(139), "A");
        tracker.addEvent(new DateTime(140), new DateTime(199), "B");

        assertEquals("A", tracker.topEventInInterval(
                new Interval(0, 200)).getDescription());
        assertEquals("A", tracker.topEventInInterval(
                new Interval(50, 119)).getDescription());
        assertEquals("B", tracker.topEventInInterval(
                new Interval(90, 119)).getDescription());
        assertEquals("B", tracker.topEventInInterval(
                new Interval(90, 119)).getDescription());
        assertEquals("B", tracker.topEventInInterval(
                new Interval(100, 130)).getDescription());
        assertEquals("A", tracker.topEventInInterval(
                new Interval(100, 139)).getDescription());
        assertEquals("B", tracker.topEventInInterval(
                new Interval(100, 141)).getDescription());
    }

    public void testTopEventInInterval_LatestEventWinsIfDraw()
            throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(1), "A");
        tracker.addEvent(new DateTime(2), new DateTime(3), "B");
        tracker.addEvent(new DateTime(4), new DateTime(5), "A");
        tracker.addEvent(new DateTime(6), new DateTime(7), "B");

        final AggregateEvent aggregate =
                tracker.topEventInInterval(new Interval(0, 10));
        assertEquals("B", aggregate.getDescription());
    }

    public void testMostRecentEvent_NoEventsInTracker() throws Exception {
        assertNull(tracker.mostRecentEvent());
    }

    public void testMostRecentEvent_SomeEventsInTracker() throws Exception {
        tracker.addEvent(new DateTime(0), new DateTime(1), "Many");
        tracker.addEvent(new DateTime(2), new DateTime(3), "Many");
        tracker.addEvent(new DateTime(4), new DateTime(5), "Many");
        tracker.addEvent(new DateTime(6), new DateTime(7), "Single");

        assertEquals(new Event(new Interval(6, 7), "Single"),
                     tracker.mostRecentEvent());
    }
}
