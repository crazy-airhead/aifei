/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.db.core;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AifeiRowTemporalTest {

    @Test
    public void localDateGetterConvertsJdbcDateAndSupportsDefaultValue() {
        LocalDate expected = LocalDate.of(2026, 8, 19);
        LocalDate defaultValue = LocalDate.of(2000, 1, 1);
        Row row = new Row().data(new LinkedHashMap<>())
                .put("birthday", java.sql.Date.valueOf(expected));

        assertEquals(expected, row.getLocalDate("birthday"));
        assertSame(defaultValue, row.getLocalDate("missing", defaultValue));
        assertNull(row.getLocalDate("missing"));
    }

    @Test
    public void localTimeGetterConvertsJdbcTimeAndSupportsDefaultValue() {
        LocalTime expected = LocalTime.of(12, 34, 56);
        LocalTime defaultValue = LocalTime.MIDNIGHT;
        Row row = new Row().data(new LinkedHashMap<>())
                .put("start_time", java.sql.Time.valueOf(expected));

        assertEquals(expected, row.getLocalTime("start_time"));
        assertSame(defaultValue, row.getLocalTime("missing", defaultValue));
        assertNull(row.getLocalTime("missing"));
    }
}
