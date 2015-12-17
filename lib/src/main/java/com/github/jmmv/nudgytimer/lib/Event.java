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
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * Representation of an event recorded by the user.
 *
 * An event is, simply put, a time interval with a description.  In some sense,
 * it can be thought of as a calendar entry in the past.
 */
public final class Event implements BaseEvent {
    /** The time interval in which the event took place. */
    private Interval interval;

    /** Free-form textual description of the event. */
    private String description;

    /**
     * Creates a new event.
     *
     * @param interval The time interval of the event.
     * @param description The description of the event.  Cannot be empty.
     *
     * @throws BadEventException If the provided details cannot represent a
     * valid event.
     */
    public Event(final Interval interval, final String description)
            throws BadEventException {
        if (description.isEmpty()) {
            throw new BadEventException("Cannot create event without " +
                                           "a description");
        }
        this.interval = interval;
        this.description = description;
    }

    /**
     * Gets the time interval of the event.
     */
    public Interval getInterval() {
        return interval;
    }

    /**
     * Gets the point in time when the event started.
     */
    public DateTime getStart() {
        return interval.getStart();
    }

    /**
     * Gets the point in time when the event ended.
     */
    public DateTime getEnd() {
        return interval.getEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration toDuration() {
        return interval.toDuration();
    }

    /**
     * Compares this event with another one for equality.
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof Event)) {
            return false;
        } else {
            final Event other = (Event) object;
            return (interval.equals(other.interval) &&
                    description.equals(other.description));
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
        result = 31 * result + interval.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    /**
     * Formats this event for display.
     */
    @Override
    public String toString() {
        return String.format("%s: %s", interval.toString(), description);
    }
}
