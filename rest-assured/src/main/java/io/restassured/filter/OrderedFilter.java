/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.filter;

/**
 * Implement this filter if you need to control the order of the filter.
 * Higher values are interpreted as lower priority. As a consequence, the object with the lowest value has the highest priority (somewhat analogous to Servlet load-on-startup values).
 * Same order values will result in arbitrary sort positions for the affected objects.
 */
public interface OrderedFilter extends Filter {

    /**
     * The default precedence
     */
    int DEFAULT_PRECEDENCE = 1000;

    /**
     * Useful constant for the highest precedence value.
     *
     * @see Integer#MIN_VALUE
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * Useful constant for the lowest precedence value.
     *
     * @see Integer#MAX_VALUE
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * @return The order of the filter
     */
    int getOrder();
}
