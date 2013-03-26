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

import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.mapper.ObjectMapperSerializationContext
import com.jayway.restassured.mapper.factory.GsonObjectMapperFactory

class GsonMapper implements ObjectMapper {

    private GsonObjectMapperFactory factory;

    public GsonMapper(GsonObjectMapperFactory factory) {
        this.factory = factory
    }

    def Object deserialize(ObjectMapperDeserializationContext context) {
        def object = context.getResponse().asString()
        def cls = context.getType()
		def gson = factory.create(cls, context.getCharset())
		return gson.fromJson(object, cls)
	}

	def Object serialize(ObjectMapperSerializationContext context) {
        def object = context.getObjectToSerialize();
		def gson = factory.create(object.getClass(), context.getCharset())
		return gson.toJson(object)
	}
}
