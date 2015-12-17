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

import org.joda.time.Duration;

/**
 * Base definition of properties shared by all event types.
 */
public interface BaseEvent {
    /** Gets the textual description of the event. */
    String getDescription();

    /** Computes the total duration of the event. */
    Duration toDuration();
}
