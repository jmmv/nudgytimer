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

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.Set;
import java.util.SortedSet;

/**
 * Interface to record and query events by a variety of criteria.
 */
public interface Tracker {
    /**
     * Records a new event.
     *
     * @param start The start time of the event.
     * @param end The end time of the event.
     * @param description The free-form textual description of the event.
     *
     * @throws BadEventException If the provided information would cause the
     * creation of an invalid event.
     */
    void addEvent(final DateTime start, final DateTime end,
                  final String description) throws BadEventException;

    /**
     * Queries all events that fall within a given interval.
     *
     * If the provided interval intersects any event, the event will be
     * included in the result but its own interval will be cut to match the
     * interval requested by the caller.
     *
     * @param interval The interval of the desired events.
     *
     * @return A collection of events sorted by their interval.  Note that
     * these events might not directly match the events recorded by the
     * user if the provided interval happens to "cut" through them.
     */
    Set<Event> eventsInInterval(final Interval interval);

    /**
     * Queries all events within a given interval and aggregates them.
     *
     * @param interval The interval of the desired events.
     *
     * @return A collection of events aggregated by their description and
     * sorted by their start time.
     */
    SortedSet<AggregateEvent> aggregateEventsInInterval(
            final Interval interval);

    /**
     * Computes the longest event within a given interval.
     *
     * If an events is repeated throughout the interval at different points in
     * time, their durations are coalesced.
     *
     * See the description of eventsInInterval for the semantics of which
     * events are included in the computation.
     *
     * @param interval The interval of the desired events.
     *
     * @return The description of the longest event.
     */
    AggregateEvent topEventInInterval(final Interval interval);

    /**
     * Fetches the most recently recorded event.
     *
     * @return The description of the most recently added event.
     */
    Event mostRecentEvent();
}
