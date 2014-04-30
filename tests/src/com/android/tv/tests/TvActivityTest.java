/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.tests;

import android.test.ActivityInstrumentationTestCase2;

import com.android.tv.TvActivity;

public class TvActivityTest extends ActivityInstrumentationTestCase2<TvActivity> {

    public TvActivityTest() {
        super(TvActivity.class);
    }

    public void testLifeCycle() {
        getActivity();
    }
}