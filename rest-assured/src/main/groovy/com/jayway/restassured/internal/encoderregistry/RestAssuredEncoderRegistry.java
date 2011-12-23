/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.internal.encoderregistry;

import com.jayway.restassured.http.EncoderRegistry;
import groovy.lang.Closure;
import com.jayway.restassured.http.ContentType;

/**
 * A custom Encoder Registry that returns a default encoder (URL ENCODED)
 * for the request body when using a custom content type.
 */
public class RestAssuredEncoderRegistry extends EncoderRegistry {

    @Override
    public Closure getAt(Object contentType) {
        final Closure closure = super.getAt(contentType);

        if(closure == null) {
            return super.getAt(ContentType.URLENC.toString());
        }
        return closure;
    }
}
