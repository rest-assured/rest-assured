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
  def serialize(Object object, String contentType) {
    JsonEncoding jsonEncoding = getEncoding(contentType);
    def mapper = new ObjectMapper();
    def stream = new ByteArrayOutputStream();
    JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(stream, jsonEncoding)
    mapper.writeValue(jsonGenerator, object);
    return stream.toString()
  }

  def deserialize(Object object, Class cls) {
    def mapper = new ObjectMapper();
    JavaType javaType = TypeFactory.type(cls)
    return mapper.readValue(object, javaType);
  }

  private JsonEncoding getEncoding(String contentType) {
    if(contentType != null && contentType.contains("charset")) {
      String charset = contentType.substring(contentType.indexOf("charset")).trim()
      for (JsonEncoding encoding : JsonEncoding.values()) {
        if (charset.equals(encoding.getJavaName())) {
          return encoding;
        }
      }
    }
    return JsonEncoding.UTF8;
  }
}