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

package io.restassured.path.xml.mapping;

import io.restassured.common.mapper.ObjectDeserializationContext;

/**
 * Interface for all XmlPath object deserializers. It's possible to roll your own implementation if the pre-defined
 * de-serializers are not enough.
 */
public interface XmlPathObjectDeserializer {
    /**
     * De-serialize data to an instance of <code>T</code>.
     */
    <T> T deserialize(ObjectDeserializationContext ctx);
}
