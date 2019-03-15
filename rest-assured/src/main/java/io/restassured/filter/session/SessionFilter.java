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

package io.restassured.filter.session;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * A session filter can be used record the session id returned from the server as well as automatically apply this session id in subsequent requests.
 * For example:
 * <pre>
 * SessionFilter sessionFilter = new SessionFilter();
 *
 * given().
 *         auth().form("John", "Doe").
 *         filter(sessionFilter).
 * expect().
 *          statusCode(200).
 * when().
 *        get("/x");
 *
 * given().
 *         filter(sessionFilter). // Reuse the same session filter instance to automatically apply the session id from the previous response
 * expect().
 *          statusCode(200).
 * when().
 *        get("/y");
 * </pre>
 */
public class SessionFilter implements Filter {

    private final AtomicReference<String> sessionId = new AtomicReference<String>();

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        if (hasStoredSessionId() && !requestHasSessionIdDefined(requestSpec)) {
            requestSpec.sessionId(sessionId.get());
        }

        final Response response = ctx.next(requestSpec, responseSpec);
        final String sessionIdInResponse = response.sessionId();

        if (isNotBlank(sessionIdInResponse)) {
            sessionId.set(sessionIdInResponse);
        }
        return response;
    }

    private boolean requestHasSessionIdDefined(FilterableRequestSpecification requestSpec) {
        return requestSpec.getCookies().hasCookieWithName(requestSpec.getConfig().getSessionConfig().sessionIdName());
    }

    private boolean hasStoredSessionId() {
        return sessionId.get() != null;
    }


    /**
     * @return The last received session id.
     */
    public String getSessionId() {
        return sessionId.get();
    }

    /**
     * @return <code>true</code> if a session id has been returned from the server and caught by the filter, <code>false</code> otherwise.
     */
    public boolean hasSessionId() {
        return isNotBlank(sessionId.get());
    }
}
