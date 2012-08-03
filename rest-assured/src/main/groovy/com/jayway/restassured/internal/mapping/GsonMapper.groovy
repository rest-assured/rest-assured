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

import com.google.gson.Gson
import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.mapper.ObjectMapperSerializationContext

class GsonMapper implements ObjectMapper {
	private static GsonFactory gsonFactory = new GsonFactory()

	def static register(GsonFactory gsonFactory) {
		GsonMapper.gsonFactory = gsonFactory
	}

	private Gson createGson(Class cls, String charset) {
		return GsonMapper.gsonFactory.createGson(cls, charset)
	}

	def Object deserialize(ObjectMapperDeserializationContext context) {
        def object = context.getObjectToDeserializeAs(String.class)
        def cls = context.getType()

		def gson = createGson(cls, context.getCharset())
		return gson.fromJson(object, cls)
	}

	def Object serialize(ObjectMapperSerializationContext context) {
        def object = context.getObjectToSerialize();
		Gson gson = createGson(object.getClass(), context.getCharset())
		return gson.toJson(object)
	}
}
