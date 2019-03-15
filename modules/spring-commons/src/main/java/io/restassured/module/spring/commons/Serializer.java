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
package io.restassured.module.spring.commons;

import io.restassured.config.EncoderConfig;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import static io.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;

public class Serializer {

    private Serializer() {
    }

    public static String serializeIfNeeded(Object object, String contentType, SpecificationConfig config) {
        return isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType,
                findEncoderCharsetOrReturnDefault(contentType, config), null, config.getObjectMapperConfig(),
                config.getEncoderConfig()) : object.toString();
    }

    public static String findEncoderCharsetOrReturnDefault(String contentType, SpecificationConfig config) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentType);
        if (charset == null) {
            EncoderConfig encoderConfig = config.getEncoderConfig();
            if (encoderConfig.hasDefaultCharsetForContentType(contentType)) {
                charset = encoderConfig.defaultCharsetForContentType(contentType);
            } else {
                charset = encoderConfig.defaultContentCharset();
            }
        }
        return charset;
    }
}
