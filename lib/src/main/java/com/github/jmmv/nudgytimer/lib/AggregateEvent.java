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

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Aggregates data of a single event spread through various instances.
 *
 * When aggregating, we accumulate the durations of the events and keep
 * the start timestamp of the latest event so that we can implement the
 * "latest event wins" policy in case of a draw.
 */
public final class AggregateEvent
        implements BaseEvent, Comparable<AggregateEvent> {
    /**
     * Description of the event.
     *
     * TODO(jmmv): We should have the functionality to aggregate events
     * with similar descriptions into one (think different capitalization or
     * typos), in which case this should contain the "best candidate" for the
     * description of all grouped events.
     */
    private final String description;

    /**
     * Key to sort intervals by ascending start time and growing duration.
     *
     * TODO(jmmv): This might belong in its own module.
     */
    private static class Key implements Comparable<Key> {
        /** The interval's start time. */
        final DateTime start;

        /** The interval's duration. */
        final Duration duration;

        /**
         * Constructor from an interval.
         *
         * @param interval The interval for which to generate the key.
         */
        Key(final Interval interval) {
            start = interval.getStart();
            duration = interval.toDuration();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(@NonNull final Key other) {
            if (start.isBefore(other.start)) {
                return -1;
            } else if (start.equals(other.start)) {
                return duration.compareTo(other.duration);
            } else {
                return 1;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof Key)) {
                return false;
            } else {
                final Key other = (Key) o;
                return (start.equals(other.start) &&
                        duration.equals(other.duration));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + start.hashCode();
            result = 31 * result + duration.hashCode();
            return result;
        }
    }

    /**
     * Collection of time intervals in which the event happened.
     */
    private final SortedMap<Key, Interval> intervals;

    /**
     * Creates an aggregated event.
     *
     * @param description The description of all events in the aggregate.
     * @param intervals Time intervals in which the event happened.
     */
    private AggregateEvent(final String description,
                           final SortedMap<Key, Interval> intervals) {
        this.description = description;
        this.intervals = intervals;
    }

    /**
     * Builder pattern to instantiate an AggregateEvent.
     */
    public static class Builder {
        /**
         * Collection of events for the aggregate.
         */
        private final List<Event> events = new LinkedList<>();

        /**
         * Creates the builder.
         *
         * @param first First event to add to the aggregate.  This is used
         * as the baseline for all other events added, in particular to
         * ensure the descriptions match.
         */
        public Builder(final Event first) {
            events.add(first);
        }

        /**
         * Accumulates another instance of this same event.
         *
         * @param other Another occurrence of the event being aggregated.
         * The description of this other event must be equal to the
         * descriptions of all other recorded events.
         *
         * @return This builder.
         */
        public Builder accumulate(final Event other) {
            final String description = events.get(0).getDescription();
            if (BuildConfig.DEBUG &&
                    !description.equals(other.getDescription())) {
                throw new AssertionError();
            }
            events.add(other);
            return this;
        }

        /**
         * Generates an AggregateEvent based on all data collected so far.
         *
         * @return The AggregateEvent object.
         */
        public AggregateEvent build() {
            final String description = events.get(0).getDescription();

            final SortedMap<Key, Interval> intervals = new TreeMap<>();
            for (final Event event : events) {
                final Key key = new Key(event.getInterval());
                if (BuildConfig.DEBUG && intervals.containsKey(key)) {
                    throw new AssertionError();
                }
                intervals.put(key, event.getInterval());
            }

            return new AggregateEvent(description, intervals);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Gets the collection of time intervals in which the event happened.
     *
     * @return A collection of intervals, sorted in ascending order by their
     * start timestamp.
     */
    public Collection<Interval> getIntervals() {
        return intervals.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration toDuration() {
        Duration duration = new Duration(0);
        for (final Interval interval : intervals.values()) {
            duration = duration.plus(interval.toDuration());
        }
        return duration;
    }

    /**
     * Gets the latest start time of all instances of the event.
     */
    public DateTime latestStart() {
        return intervals.get(intervals.lastKey()).getStart();
    }

    /**
     * Compares this aggregate to another one.
     *
     * We consider an aggregate to be larger than another if its duration
     * is longer or, if they are of the same duration, it happens to start
     * at a later time.
     *
     * @param other The other aggregate to compare to.
     *
     * @return -1 if this aggregate is shorter than the other one; 0 if they
     * are equal; 1 if this is larger than the other one.
     */
    @Override
    public int compareTo(@NonNull final AggregateEvent other) {
        final Duration duration = toDuration();
        final Duration otherDuration = other.toDuration();

        if (duration.compareTo(otherDuration) < 0) {
            return -1;
        } else if (duration.compareTo(otherDuration) == 0) {
            final int result = latestStart().compareTo(other.latestStart());
            if (BuildConfig.DEBUG && result == 0 && !equals(other)) {
                throw new AssertionError("compareTo is inconsistent with " +
                                                 "equals");
            }
            return result;
        } else {
            if (BuildConfig.DEBUG && duration.compareTo(otherDuration) <= 0) {
                throw new AssertionError("Bad conditional flow");
            }
            return 1;
        }
    }

    /**
     * Compares this aggregate with another one for equality.
     *
     * @param object The object to compare to, which should be an AggregateEvent
     * object.
     *
     * @return True if the two objects are of the same type and have the same
     * contents; false otherwise.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof AggregateEvent)) {
            return false;
        } else {
            final AggregateEvent other = (AggregateEvent) object;
            return (description.equals(other.description) &&
                    intervals.equals(other.intervals));
        }
    }

    /**
     * Returns a unique hash code for the object.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + description.hashCode();
        for (final Interval interval : intervals.values()) {
            result = 31 * result + interval.hashCode();
        }
        return result;
    }

    /**
     * Formats this aggregate for display.
     *
     * @return A textual representation of the object for human consumption.
     */
    @Override
    public String toString() {
        String text = description + ":";
        for (final Interval interval : intervals.values()) {
            text += " " + interval.toString();
        }
        return text;
    }
}
