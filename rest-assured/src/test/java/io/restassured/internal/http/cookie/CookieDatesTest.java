/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.internal.http.cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookieDatesTest {
    private static final Date EXPECTED = Date.from(ZonedDateTime.of(2099, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT")).toInstant());

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Wed, 21 Oct 2099 07:28:00 GMT",
            "Wednesday, 21-Oct-99 07:28:00 GMT",
            "Wed Oct 21 07:28:00 2099",
            "Wed, 21-Oct-2099 07:28:00 GMT"})
    void every_standard_format_is_read(String value) {
        assertThat(CookieDates.parse(value, CookieDates.STANDARD_PATTERNS)).isEqualTo(EXPECTED);
    }

    @Test
    void a_value_that_matches_only_the_start_of_a_pattern_is_read() {
        assertThat(CookieDates.parse("Wed, 21 Oct 2099 07:28:00 GMT and then some", CookieDates.STANDARD_PATTERNS)).isEqualTo(EXPECTED);
    }

    @Test
    void surrounding_single_quotes_are_removed() {
        assertThat(CookieDates.parse("'Wed, 21 Oct 2099 07:28:00 GMT'", CookieDates.STANDARD_PATTERNS)).isEqualTo(EXPECTED);
    }

    @Test
    void a_two_digit_year_is_a_year_of_this_century() {
        assertThat(CookieDates.parse("Wed, 21-Oct-15 07:28:00 GMT", CookieDates.STANDARD_PATTERNS))
                .isEqualTo(Date.from(ZonedDateTime.of(2015, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT")).toInstant()));
        assertThat(CookieDates.parse("Wed, 21-Oct-99 07:28:00 GMT", CookieDates.STANDARD_PATTERNS))
                .isEqualTo(Date.from(ZonedDateTime.of(2099, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT")).toInstant()));
    }

    @Test
    void a_value_without_a_time_zone_is_read_as_gmt() {
        assertThat(CookieDates.parse("21 Oct 2099 07:28:00", List.of("dd MMM yyyy HH:mm:ss"))).isEqualTo(EXPECTED);
    }

    @Test
    void a_value_that_no_pattern_reads_is_null() {
        assertThat(CookieDates.parse("NO DATE!", CookieDates.STANDARD_PATTERNS)).isNull();
        assertThat(CookieDates.parse("", CookieDates.STANDARD_PATTERNS)).isNull();
        assertThat(CookieDates.parse(null, CookieDates.STANDARD_PATTERNS)).isNull();
    }

    @Test
    void the_patterns_are_tried_in_order() {
        assertThat(CookieDates.parse("Wed, 21 Oct 2099 07:28:00 GMT", List.of("'nope'", CookieDates.RFC_1123))).isEqualTo(EXPECTED);
        assertThat(CookieDates.parse("Wed, 21 Oct 2099 07:28:00 GMT", List.of())).isNull();
    }
}
