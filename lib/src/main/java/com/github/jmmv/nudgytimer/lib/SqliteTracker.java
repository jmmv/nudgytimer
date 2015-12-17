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

import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A tracker that is backed by a SQLite database.
 *
 * TODO(jmmv): The operations performed by this module are potentially expensive
 * and we are currently running them on the main thread.  This is bad and it
 * should be fixed.  For now, I'll assume that any users of this app will only
 * run it for a few days in a row as suggested in the description, which should
 * prevent the store growing large and causing a noticeable slowdown.
 */
public final class SqliteTracker implements Tracker {
    /** Writable database backing this tracker. */
    private final SQLiteDatabase database;

    /**
     * Constructs a new tracker.
     *
     * @param database An open, writable database to use for this tracker.
     */
    public SqliteTracker(final SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvent(final DateTime start, final DateTime end,
                         final String description)
            throws BadEventException {
        Event event;
        try {
            final Interval interval = new Interval(start, end);
            event = new Event(interval, description);
        } catch (final IllegalArgumentException e) {
            throw new BadEventException(e.getMessage());
        }
        StoreContract.putEvent(database, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Event> eventsInInterval(final Interval interval) {
        return StoreContract.queryEvents(database, interval, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<AggregateEvent> aggregateEventsInInterval(
            final Interval interval) {
        // The majority of this computation could probably be done more
        // efficiently by a direct SQL query.  However, such query would be
        // unable to process any events that happen to be cut by the
        // queried interval, and we would need to special-case those.  This
        // sounds more complex and fragile than is otherwise done by this simple
        // code, so it will not be done unless this is proven to be a
        // performance problem.

        final Set<Event> events = eventsInInterval(interval);

        final Map<String, AggregateEvent.Builder> builders = new HashMap<>();
        for (final Event event : events) {
            final String key = event.getDescription();

            if (builders.containsKey(key)) {
                builders.get(key).accumulate(event);
            } else {
                builders.put(key, new AggregateEvent.Builder(event));
            }
        }

        final SortedSet<AggregateEvent> aggregates = new TreeSet<>();
        for (final AggregateEvent.Builder builder : builders.values()) {
            aggregates.add(builder.build());
        }
        return aggregates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregateEvent topEventInInterval(final Interval interval) {
        final SortedSet<AggregateEvent> aggregates =
                aggregateEventsInInterval(interval);
        if (aggregates.isEmpty()) {
            return null;
        } else {
            final Iterator<AggregateEvent> iterator = aggregates.iterator();
            AggregateEvent max = iterator.next();
            while (iterator.hasNext()) {
                final AggregateEvent next = iterator.next();
                if (next.compareTo(max) == 1) {
                    max = next;
                }
            }
            return max;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event mostRecentEvent() {
        // TODO(jmmv): The return value should be the combination of all "most
        // recent" events with the same description, just in case the user
        // happens to record the same event consecutively more than once.  This
        // would be to let the UI display the total time spent doing the latest
        // task.  Should either return an event that combines all consecutive
        // intervals or an aggregate.
        final Set<Event> events = StoreContract.queryEvents(
                database, TimeUtils.intervalForever(), 1L);
        if (events.isEmpty()) {
            return null;
        } else {
            return events.iterator().next();
        }
    }
}
