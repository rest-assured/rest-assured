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
 * The HTTP version a client uses.
 */
public enum HttpVersion {
    /**
     * HTTP/1.1 only.
     */
    HTTP_1_1,
    /**
     * HTTP/2 where the server supports it, HTTP/1.1 otherwise.
     */
    HTTP_2
}
