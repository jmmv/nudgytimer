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
 * Type-safe accessors for the user-customizable, application-wide settings.
 *
 * Clients should use this interface instead of caching the actual values of
 * the settings they need in order to always get the latest values configured
 * by the user.
 */
public interface Settings {
    /** Gets the polling period setting. */
    Duration getPollPeriod();
}
