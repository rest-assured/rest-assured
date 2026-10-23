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

package io.restassured.common.spi.http;

/**
 * How REST Assured uses the clients of an {@link HttpClientProvider}.
 */
public enum ClientSharing {
    /**
     * One client for each distinct {@link ClientSettings}, shared by requests and threads.
     */
    SHARED,
    /**
     * A new client for each request, closed when REST Assured is done with the response.
     */
    PER_REQUEST
}
