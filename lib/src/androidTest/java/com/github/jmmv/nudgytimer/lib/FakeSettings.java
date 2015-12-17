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

class FakeSettings implements Settings {
    /** Value for the poll period setting. */
    private Duration pollPeriod = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration getPollPeriod() {
        if (pollPeriod == null) {
            throw new AssertionError("Test value never set");
        }
        return pollPeriod;
    }

    /**
     * Sets the value of the fake poll period property.
     *
     * @param duration The fake value.
     */
    public void setPollPeriod(final Duration duration) {
        pollPeriod = duration;
    }
}
