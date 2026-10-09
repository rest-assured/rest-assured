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

package io.restassured.internal.http;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Configures a request sent by {@link HTTPBuilder#request(String, Object, boolean, RequestConfigurer)}.
 * <p>
 * Both checked exceptions are declared because a Groovy closure coerced to this interface would otherwise have them
 * wrapped in an {@link java.lang.reflect.UndeclaredThrowableException}.
 */
@FunctionalInterface
public interface RequestConfigurer {

    /**
     * @param request the request to configure: its uri, headers, content-type, body and response handlers
     */
    void configure(HTTPBuilder.RequestConfigDelegate request) throws IOException, URISyntaxException;
}
