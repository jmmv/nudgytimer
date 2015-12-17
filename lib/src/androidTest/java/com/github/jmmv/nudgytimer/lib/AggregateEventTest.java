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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class AggregateEventTest extends TestCase {
    public void testBuilderAndGetters() throws Exception {
        final Event first = new Event(new Interval(10, 30), "Foo");

        final AggregateEvent.Builder builder =
                new AggregateEvent.Builder(first);

        final AggregateEvent aggregate1 = builder.build();
        assertEquals("Foo", aggregate1.getDescription());

        final Collection<Interval> intervals1 = new LinkedList<>();
        intervals1.add(first.getInterval());
        assertTrue(Arrays.equals(intervals1.toArray(),
                                 aggregate1.getIntervals().toArray()));

        final Event second = new Event(new Interval(60, 100), "Foo");
        final Event third = new Event(new Interval(5, 8), "Foo");

        builder.accumulate(second).accumulate(third);
        final AggregateEvent aggregate2 = builder.build();
        assertEquals("Foo", aggregate2.getDescription());

        final Collection<Interval> intervals2 = new LinkedList<>();
        intervals2.add(third.getInterval());
        intervals2.add(first.getInterval());
        intervals2.add(second.getInterval());
        assertTrue(Arrays.equals(intervals2.toArray(),
                                 aggregate2.getIntervals().toArray()));
    }

    public void testGetIntervals_Sorted() throws Exception {
        final Event event1 = new Event(new Interval(10, 30), "Foo");
        final Event event2 = new Event(new Interval(50, 51), "Foo");
        final Event event3 = new Event(new Interval(10, 20), "Foo");
        final Event event4 = new Event(new Interval(30, 40), "Foo");

        final AggregateEvent aggregate =
                new AggregateEvent.Builder(event1)
                        .accumulate(event2)
                        .accumulate(event3)
                        .accumulate(event4)
                        .build();

        final Collection<Interval> intervals = new LinkedList<>();
        intervals.add(event3.getInterval());
        intervals.add(event1.getInterval());
        intervals.add(event4.getInterval());
        intervals.add(event2.getInterval());

        assertTrue(Arrays.equals(intervals.toArray(),
                                 aggregate.getIntervals().toArray()));
    }

    public void testToDuration() throws Exception {
        final Event first = new Event(new Interval(10, 30), "A");
        final Event second = new Event(new Interval(40, 50), "A");
        final Event third = new Event(new Interval(80, 100), "A");

        final AggregateEvent aggregate =
                new AggregateEvent.Builder(first).accumulate(
                        second).accumulate(third).build();
        assertEquals(50, aggregate.toDuration().getMillis());
    }

    public void testLatestStart() throws Exception {
        final Event first = new Event(new Interval(10, 30), "A");
        final Event second = new Event(new Interval(80, 200), "A");
        final Event third = new Event(new Interval(70, 500), "A");

        final AggregateEvent aggregate =
                new AggregateEvent.Builder(first).accumulate(
                        second).accumulate(third).build();
        assertEquals(new DateTime(80), aggregate.latestStart());
    }

    public void testCompareTo() throws Exception {
        final AggregateEvent aggregate1 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).build();
        final AggregateEvent aggregate2 = new AggregateEvent.Builder(
                new Event(new Interval(20, 50), "B")).build();
        final AggregateEvent aggregate3 = new AggregateEvent.Builder(
                new Event(new Interval(30, 60), "C")).build();

        assertEquals( 0, aggregate1.compareTo(aggregate1));
        assertEquals(-1, aggregate1.compareTo(aggregate2));
        assertEquals(-1, aggregate1.compareTo(aggregate3));

        assertEquals( 1, aggregate2.compareTo(aggregate1));
        assertEquals( 0, aggregate2.compareTo(aggregate2));
        assertEquals(-1, aggregate2.compareTo(aggregate3));

        assertEquals( 1, aggregate3.compareTo(aggregate1));
        assertEquals( 1, aggregate3.compareTo(aggregate2));
        assertEquals( 0, aggregate3.compareTo(aggregate3));
    }

    public void testEquals() throws Exception {
        final AggregateEvent aggregate1 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).build();
        final AggregateEvent aggregate1Bis = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).build();
        final AggregateEvent aggregate2 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "B")).build();
        final AggregateEvent aggregate3 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).accumulate(
                new Event(new Interval(30, 40), "A")).build();

        //noinspection ObjectEqualsNull
        assertFalse(aggregate1.equals(null));
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(aggregate1.equals(Long.valueOf(10)));

        assertEquals(aggregate1, aggregate1);
        assertEquals(aggregate1, aggregate1Bis);
        assertFalse(aggregate1.equals(aggregate2));
        assertFalse(aggregate1.equals(aggregate3));
        assertFalse(aggregate2.equals(aggregate3));
    }

    public void testHashcode() throws Exception {
        final AggregateEvent aggregate1 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).build();
        final AggregateEvent aggregate1Bis = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).build();
        final AggregateEvent aggregate2 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "B")).build();
        final AggregateEvent aggregate3 = new AggregateEvent.Builder(
                new Event(new Interval(10, 20), "A")).accumulate(
                new Event(new Interval(30, 40), "A")).build();

        assertTrue(aggregate1.hashCode() == aggregate1.hashCode());
        assertTrue(aggregate1.hashCode() == aggregate1Bis.hashCode());

        assertFalse(aggregate1.hashCode() == aggregate2.hashCode());
        assertFalse(aggregate1.hashCode() == aggregate3.hashCode());
        assertFalse(aggregate2.hashCode() == aggregate3.hashCode());
    }

    public void testToString() throws Exception {
        final Interval interval1 = new Interval(10, 20);
        final Interval interval2 = new Interval(30, 40);
        final AggregateEvent aggregate1 = new AggregateEvent.Builder(
                new Event(interval1, "A")).build();
        final AggregateEvent aggregate2 = new AggregateEvent.Builder(
                new Event(interval1, "B")).accumulate(
                new Event(interval2, "B")).build();

        assertEquals(String.format("A: %s", interval1.toString()),
                     aggregate1.toString());
        assertEquals(String.format("B: %s %s", interval1.toString(),
                                   interval2.toString()),
                     aggregate2.toString());
    }
}
