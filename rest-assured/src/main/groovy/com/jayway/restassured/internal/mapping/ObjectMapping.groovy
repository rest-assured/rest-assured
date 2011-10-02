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

import com.google.gson.Gson
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlRootElement
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.type.TypeFactory
import org.codehaus.jackson.type.JavaType

class ObjectMapping {
  private static final boolean isJacksonPresent = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator");

  private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder");

  private static final boolean isGsonPresent = existInCP("com.google.gson.Gson");

  private static boolean existInCP(String className) {
    try {
      Class.forName(className, false, Thread.currentThread().getContextClassLoader())
      return true
    } catch(Exception e) {
      return false
    }
  }

  public static <T> T deserialize(Object object, Class<T> cls, String contentType) {
    def ct = contentType.toLowerCase()
    if(ct.contains("json")) {
      if(isJacksonPresent) {
        return parseWithJackson(object, cls)
      } else if(isGsonPresent) {
        return parseWithGson(object, cls)
      }
      throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson or Gson in the classpath.")
    } else if(ct.contains("xml")) {
      if(isJaxbPresent) {
        return parseWithJaxb(object, cls);
      }
      throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
    }
    throw new IllegalStateException("Cannot parse object because no support XML or JSON deserializer was found in classpath.")
  }

  static def parseWithJaxb(Object object, Class cls) {
    JAXBContext jaxbContext = JAXBContext.newInstance(cls);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    def reader = new StringReader(object)
    if (cls.isAnnotationPresent(XmlRootElement.class)) {
      return unmarshaller.unmarshal(reader);
    } else {
      JAXBElement jaxbElement = unmarshaller.unmarshal(reader, cls);
      return jaxbElement.getValue();
    }
  }

  private static def parseWithGson(object, Class cls) {
    def gson = new Gson();
    return gson.fromJson(object, cls)
  }

  static def parseWithJackson(Object object, Class cls) {
    def mapper = new ObjectMapper();
    JavaType javaType = TypeFactory.type(cls)
    return mapper.readValue(object, javaType);
  }
}
