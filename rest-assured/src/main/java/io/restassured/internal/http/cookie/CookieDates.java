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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Parses the date of an {@code Expires} cookie attribute, as REST Assured has always done it: with
 * {@link SimpleDateFormat} in {@link Locale#US} and GMT, trying one pattern after the other and taking the first one
 * that reads something, so a date that only partly matches a pattern is accepted. A two digit year is read as a year
 * of this century, and a four digit pattern reads a two digit year as that year, so {@code 99} means the year 99.
 */
public final class CookieDates {
    /**
     * The date format of RFC 1123, which {@code Expires} should use.
     */
    public static final String RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    /**
     * The date format of RFC 1036, with a two digit year.
     */
    public static final String RFC_1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
    /**
     * The date format of C's {@code asctime()}.
     */
    public static final String ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    /**
     * The formats of {@code Expires} that every cookie may use: RFC 1123, RFC 1036 and {@code asctime()}.
     */
    public static final List<String> STANDARD_PATTERNS = List.of(RFC_1123, RFC_1036, ASCTIME);

    private static final Date TWO_DIGIT_YEAR_START;

    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        calendar.clear();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        TWO_DIGIT_YEAR_START = calendar.getTime();
    }

    private CookieDates() {
    }

    /**
     * @param value    the value of the {@code Expires} attribute, which may be surrounded by single quotes
     * @param patterns the date patterns to try, in order
     * @return the date, or {@code null} if no pattern reads the value
     */
    public static Date parse(String value, List<String> patterns) {
        if (value == null) {
            return null;
        }
        String date = value;
        if (date.length() > 1 && date.startsWith("'") && date.endsWith("'")) {
            date = date.substring(1, date.length() - 1);
        }
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            format.set2DigitYearStart(TWO_DIGIT_YEAR_START);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(date, position);
            if (position.getIndex() != 0) {
                return parsed;
            }
        }
        return null;
    }
}
