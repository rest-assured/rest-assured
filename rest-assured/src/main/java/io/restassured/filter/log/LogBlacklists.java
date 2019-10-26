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

package io.restassured.filter.log;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class represents a container of blacklists, to be used during logging, of key-based components of an HTTP request.
 */
public class LogBlacklists {

    private Set<String> headersBlacklist;

    public LogBlacklists() {
        headersBlacklist = new LinkedHashSet<>();
    }

    /**
     * Blacklists a header given its name
     * @param headerName of the header to be blacklisted
     * @return LogBlacklists
     */
    public LogBlacklists blacklistHeader(final String headerName) {
        headersBlacklist.add(headerName);
        return this;
    }

    /**
     * Returns the set of blacklisted headers
     * @return set of blacklisted headers
     */
    public Set<String> getHeadersBlacklist() {
        return Collections.unmodifiableSet(headersBlacklist);
    }
}
