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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages the lifecycle of our store.
 */
public class StoreHelper extends SQLiteOpenHelper {
    /**
     * Constructor.
     *
     * @param context An application context.
     * @param name The name of the database.  If null, creates an in-memory
     * database.
     */
    private StoreHelper(Context context, String name) {
        super(context, name, null, StoreContract.SCHEMA_VERSION);
    }

    /**
     * Creates a new helper to access an in-memory database.
     *
     * @param context An application context.
     *
     * @return A new helper.
     */
    public static StoreHelper newInMemory(Context context) {
        return new StoreHelper(context, null);
    }

    /**
     * Creates a new helper to access a persistent database.
     *
     * @param context An application context.
     *
     * @return A new helper.
     */
    public static StoreHelper newOnDisk(Context context) {
        return new StoreHelper(context,
                               context.getString(R.string.database_name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        for (final String statement : StoreContract.CREATE_DB_SQL) {
            database.execSQL(statement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpgrade(SQLiteDatabase database,
                          int oldVersion, int newVersion) {
        throw new AssertionError("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDowngrade(SQLiteDatabase database,
                            int oldVersion, int newVersion) {
        throw new AssertionError("Not implemented");
    }
}
