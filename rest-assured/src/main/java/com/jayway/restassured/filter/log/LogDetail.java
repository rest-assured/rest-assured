/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.filter.log;

/**
 * The different log details. Determine what should be logged in a request or response.
 */
public enum LogDetail {
    /**
     * Logs everything
     */
    ALL,
    /**
     * Log only headers
     */
    HEADERS,
    /**
     * Log only cookies
     */
    COOKIES,
    /**
     * Log on the body
     */
    BODY,
    /**
     * Log only the status line (works only for responses).
     */
    STATUS,
    /**
     * Logs only the request parameters (only works for requests)
     */
    PARAMS
}
