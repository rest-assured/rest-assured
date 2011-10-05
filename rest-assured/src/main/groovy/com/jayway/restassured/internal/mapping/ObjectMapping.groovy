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

import static com.jayway.restassured.assertion.AssertParameter.notNull
import static groovyx.net.http.ContentType.ANY
import com.jayway.restassured.parsing.Parser

class ObjectMapping {
  private static final boolean isJacksonPresent = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator");

  private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder");

  private static final boolean isGsonPresent = existInCP("com.google.gson.Gson");

  private static boolean existInCP(String className) {
    try {
      Class.forName(className, false, Thread.currentThread().getContextClassLoader())
      return true
    } catch(Throwable e) {
      return false
    }
  }

  public static <T> T deserialize(Object object, Class<T> cls, String contentType, String defaultContentType) {
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
    } else if(defaultContentType != null){
      if(defaultContentType.contains("json")) {
        if(isJacksonPresent) {
          return parseWithJackson(object, cls)
        } else if(isGsonPresent) {
          return parseWithGson(object, cls)
        }
      } else if(defaultContentType.contains("xml")) {
        if(isJaxbPresent) {
          return parseWithJaxb(object, cls);
        }
      }
    }
    throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was not specified in response. Content-Type was '%s'.", contentType))
  }

  public static String serialize(Object object, String contentType) {
    notNull(object, "Object to serialize")
    if(contentType == null || contentType == ANY.toString()) {
      if(isJacksonPresent) {
        return serializeWithJackson(object, contentType)
      } else if(isGsonPresent) {
        return serializeWithGson(object, contentType)
      } else if(isJaxbPresent) {
        return serializeWithJaxb(object, contentType)
      }
      throw new IllegalArgumentException("Cannot serialize because no JSON or XML serializer found in classpath.")
    } else {
      def ct = contentType.toLowerCase()
      if(ct.contains("json")) {
        if(isJacksonPresent) {
          return serializeWithJackson(object, contentType)
        } else if(isGsonPresent) {
          return serializeWithGson(object, contentType)
        }
        throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put either Jackson or Gson in the classpath.")
      } else if(ct.contains("xml")) {
        if(isJaxbPresent) {
          return serializeWithJaxb(object, contentType);
        } else {
          throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
        }
      } else {
        throw new IllegalArgumentException("Cannot serialize because cannot determine how to serialize content-type $contentType")
      }
    }

    return serializeWithJaxb(object, contentType)
  }

  static String serializeWithGson(Object object, String contentType) {
    new GsonMapping().serialize(object, contentType)
  }

  static String serializeWithJackson(Object object, String contentType) {
    new JacksonMapping().serialize(object, contentType)
  }

  static String serializeWithJaxb(object, String contentType) {
    new JaxbMapping().serialize(object, contentType)
  }

  static def parseWithJaxb(Object object, Class cls) {
    new JaxbMapping().deserialze(object, cls)
  }

  private static def parseWithGson(object, Class cls) {
    new GsonMapping().deserialze(object, cls)
  }

  static def parseWithJackson(Object object, Class cls) {
    new JacksonMapping().deserialize(object, cls)
  }
}
