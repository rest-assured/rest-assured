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

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Encodes a request body for a given request content-type, see {@link EncoderRegistry}.
 */
@FunctionalInterface
public interface RequestBodyEncoder {

    /**
     * @param contentType the request content-type
     * @param body        the request body
     * @return an {@link HttpEntity} encapsulating the request body
     */
    HttpEntity encode(Object contentType, Object body) throws IOException;
}
