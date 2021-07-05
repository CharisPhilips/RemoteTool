/*
 * Copyright 2015-2019 OpenIndex.de.
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
package com.djelloul.core.monitor;

import java.util.Date;

/**
 * Statistical record about data transfer.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public class DataSample {
    public final long byteCount;
    public final Date start;
    public final Date end;

    public DataSample(long byteCount, Date start, Date end) {
        this.byteCount = byteCount;
        this.start = start;
        this.end = end;
    }
}
