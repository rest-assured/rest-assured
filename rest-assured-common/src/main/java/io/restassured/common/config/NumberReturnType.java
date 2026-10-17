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

package io.restassured.common.config;

/**
 * The type that JSON numbers are returned as.
 * <p>
 * Non-integer numbers can be returned as:
 * <ul>
 * <li>FLOAT_AND_DOUBLE (default)</li>
 * <li>BIG_DECIMAL</li>
 * <li>DOUBLE</li>
 * </ul>
 * Non-decimal numbers can be returned as BIG_INTEGER instead of int and long.
 */
public enum NumberReturnType {
    /**
     * Convert all non-integer numbers to floats and doubles (depending on the size of the number)
     */
    FLOAT_AND_DOUBLE,
    /**
     * Convert all non-integer numbers to BigDecimal
     */
    BIG_DECIMAL,
    /**
     * Convert all non-integer numbers to doubles
     */
    DOUBLE,
    /**
     * Converts all non-decimal numbers to BigInteger
     */
    BIG_INTEGER;

    /**
     * Returns a boolean indicating whether this type is included in those that deal with floats
     * or doubles exclusive of BigDecimal.
     *
     * @return <code>true</code> if value is {@link #FLOAT_AND_DOUBLE} or {@link #DOUBLE}, <code>false</code> otherwise.
     */
    public final boolean isFloatOrDouble() {
        return this == FLOAT_AND_DOUBLE || this == DOUBLE;
    }
}
