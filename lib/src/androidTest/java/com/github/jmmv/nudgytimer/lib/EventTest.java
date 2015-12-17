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

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.Interval;

public class EventTest extends TestCase {
    public void testGetters() throws Exception {
        final Event event = new Event(new Interval(50, 60), "Foo");
        assertEquals(new Interval(50, 60), event.getInterval());
        assertEquals(new DateTime(50), event.getStart());
        assertEquals(new DateTime(60), event.getEnd());
        assertEquals("Foo", event.getDescription());
        assertEquals(10, event.toDuration().getMillis());
    }

    public void testEquals() throws Exception {
        final Event event1 = new Event(new Interval(50, 60), "A");
        final Event event1Bis = new Event(new Interval(50, 60), "A");
        final Event event2 = new Event(new Interval(50, 65), "A");
        final Event event3 = new Event(new Interval(50, 60), "B");

        //noinspection ObjectEqualsNull
        assertFalse(event1.equals(null));
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(event1.equals(new DateTime(10)));

        assertEquals(event1, event1);
        assertEquals(event1, event1Bis);

        assertFalse(event1.equals(event2));
        assertFalse(event1.equals(event3));
        assertFalse(event2.equals(event3));
    }

    public void testHashcode() throws Exception {
        final Event event1 = new Event(new Interval(50, 60), "A");
        final Event event1Bis = new Event(new Interval(50, 60), "A");
        final Event event2 = new Event(new Interval(50, 65), "A");
        final Event event3 = new Event(new Interval(50, 60), "B");

        assertTrue(event1.hashCode() == event1.hashCode());
        assertTrue(event1.hashCode() == event1Bis.hashCode());

        assertFalse(event1.hashCode() == event2.hashCode());
        assertFalse(event1.hashCode() == event3.hashCode());
        assertFalse(event2.hashCode() == event3.hashCode());
    }

    public void testToString() throws Exception {
        final Interval interval = new Interval(10000, 100000000);
        final Event event = new Event(interval, "The text");
        assertEquals(String.format("%s: The text", interval.toString()),
                     event.toString());
    }
}
