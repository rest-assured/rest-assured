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




package io.restassured.internal.common.mapper

import io.restassured.common.mapper.DataToDeserialize
import io.restassured.common.mapper.ObjectDeserializationContext

import java.lang.reflect.Type

class ObjectDeserializationContextImpl implements ObjectDeserializationContext {

    def DataToDeserialize dataToDeserialize
    def Type type
    def charset

    @Override
    DataToDeserialize getDataToDeserialize() {
        return dataToDeserialize
    }

    @Override
    Type getType() {
        return type
    }

    @Override
    String getCharset() {
        return charset
    }
}
