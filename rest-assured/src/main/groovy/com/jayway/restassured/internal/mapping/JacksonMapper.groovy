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

package com.jayway.restassured.internal.mapping

import org.codehaus.jackson.JsonEncoding
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.type.TypeFactory
import org.codehaus.jackson.type.JavaType

import com.jayway.restassured.internal.http.CharsetExtractor

class JacksonMapper implements com.jayway.restassured.internal.mapping.ObjectMapper {
	private static ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory()

	def static register(ObjectMapperFactory objectMapperFactory) {
		JacksonMapper.objectMapperFactory = objectMapperFactory
	}

	private ObjectMapper createJacksonObjectMapper(Class cls) {
		return JacksonMapper.objectMapperFactory.createJacksonObjectMapper(cls)
	}

	def String serialize(Object object, String contentType) {
		JsonEncoding jsonEncoding = getEncoding(contentType)

		def mapper = createJacksonObjectMapper(object.getClass())
		def stream = new ByteArrayOutputStream()
		JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(stream, jsonEncoding)
		mapper.writeValue(jsonGenerator, object)
		return stream.toString()
	}

	def Object deserialize(Object object, Class cls) {
		def mapper = createJacksonObjectMapper(cls)
		JavaType javaType = TypeFactory.type(cls)
		return mapper.readValue(object, javaType)
	}

	private JsonEncoding getEncoding(String contentType) {
		def foundEncoding = JsonEncoding.UTF8
		if(contentType != null) {
			def charset = CharsetExtractor.getCharsetFromContentType(contentType)
			if(charset != null) {
				for (JsonEncoding encoding : JsonEncoding.values()) {
					if (charset.equals(encoding.getJavaName())) {
						foundEncoding = encoding
						break
					}
				}
			}
		}
		return foundEncoding
	}
}