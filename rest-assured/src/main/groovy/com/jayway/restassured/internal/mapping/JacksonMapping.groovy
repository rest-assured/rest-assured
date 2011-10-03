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

class JacksonMapping {
  def serialize(Object object, String encoding) {
    JsonEncoding jsonEncoding = null;
    if (encoding != null) {
      for (JsonEncoding value : JsonEncoding.values()) {
        if (encoding.equals(value.getJavaName())) {
          jsonEncoding = value;
          break;
        }
      }
    }
    jsonEncoding = jsonEncoding ?: JsonEncoding.UTF8
    def mapper = new ObjectMapper();
    def writer = new StringWriter()
    JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(writer)
    mapper.writeValue(writer, object);
    return writer.toString()
  }

  def deserialize(Object object, Class cls) {
    def mapper = new ObjectMapper();
    JavaType javaType = TypeFactory.type(cls)
    return mapper.readValue(object, javaType);
  }
}