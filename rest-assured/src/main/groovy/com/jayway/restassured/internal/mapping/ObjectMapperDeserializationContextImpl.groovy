/*
 * Copyright 2012 the original author or authors.
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
package com.jayway.restassured.internal.mapping

import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.response.ResponseBodyData

class ObjectMapperDeserializationContextImpl implements ObjectMapperDeserializationContext {

    def ResponseBodyData responseData
    def Class<?> type
    def contentType
    def charset

    @Override
    ResponseBodyData getResponse() {
        return responseData
    }

    @Override
    Class<?> getType() {
        return type
    }

    @Override
    String getContentType() {
        return contentType
    }

    @Override
    String getCharset() {
        return charset
    }
}
