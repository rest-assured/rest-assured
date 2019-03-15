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

package io.restassured.common.mapper;

import java.io.InputStream;

public interface DataToDeserialize {
    /**
     * Get the data as a string.
     *
     * @return The data as a string.
     */
    String asString();

    /**
     * Get the data as a byte array.
     *
     * @return The data as a array.
     */
    byte[] asByteArray();

    /**
     * Get the data as an input stream.
     *
     * @return The data as an input stream.
     */
    InputStream asInputStream();
}
