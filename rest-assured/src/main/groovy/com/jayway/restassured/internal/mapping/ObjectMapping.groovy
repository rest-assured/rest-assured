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

import static com.jayway.restassured.assertion.AssertParameter.notNull
import static com.jayway.restassured.http.ContentType.ANY
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase

import com.jayway.restassured.internal.http.CharsetExtractor
import com.jayway.restassured.mapper.ObjectMapperType
import com.jayway.restassured.mapper.ObjectMapper

class ObjectMapping {
	private static final boolean isJacksonPresent = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator")

	private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder")

	private static final boolean isGsonPresent = existInCP("com.google.gson.Gson")

	private static boolean existInCP(String className) {
		try {
			Class.forName(className, false, Thread.currentThread().getContextClassLoader())
			return true
		} catch(Throwable e) {
			return false
		}
	}

	public static <T> T deserialize(Object object, Class<T> cls, String contentType, String defaultContentType, ObjectMapperType mapperType) {
		String charset = CharsetExtractor.getCharsetFromContentType(contentType)
		if(mapperType != null) {
			return deserializeWithObjectMapper(object, charset, cls, mapperType)
		}
		if(containsIgnoreCase(contentType, "json")) {
			if(isJacksonPresent) {
				return parseWithJackson(object, charset, cls)
			} else if(isGsonPresent) {
				return parseWithGson(object, charset, cls)
			}
			throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson or Gson in the classpath.")
		} else if(containsIgnoreCase(contentType, "xml")) {
			if(isJaxbPresent) {
				return parseWithJaxb(object, charset, cls)
			}
			throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
		} else if(defaultContentType != null){
			if(containsIgnoreCase(defaultContentType, "json")) {
				if(isJacksonPresent) {
					return parseWithJackson(object, charset, cls)
				} else if(isGsonPresent) {
					return parseWithGson(object, charset, cls)
				}
			} else if(containsIgnoreCase(defaultContentType, "xml")) {
				if(isJaxbPresent) {
					return parseWithJaxb(object, charset, cls)
				}
			}
		}
		throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was not specified in response. Content-Type was '%s'.", contentType))
	}

	private static <T> T deserializeWithObjectMapper(Object object, String charset, Class<T> cls, ObjectMapperType mapperType) {
		if(mapperType == ObjectMapper.JACKSON && isJacksonPresent) {
			return parseWithJackson(object, charset, cls)
		} else if(mapperType == ObjectMapper.GSON && isGsonPresent) {
			return parseWithGson(object, charset, cls)
		} else if(mapperType == ObjectMapper.JAXB && isJaxbPresent) {
			return parseWithJaxb(object, charset, cls)
		} else {
			def lowerCase = mapperType.toString().toLowerCase()
			throw new IllegalArgumentException("Cannot map response body with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
		}
	}

	public static String serialize(Object object, String contentType, ObjectMapperType mapperType) {
		notNull(object, "Object to serialize")
		if(mapperType != null) {
			return serializeWithObjectMapper(object, contentType, mapperType)
		}

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
			if(containsIgnoreCase(ct, "json")) {
				if(isJacksonPresent) {
					return serializeWithJackson(object, contentType)
				} else if(isGsonPresent) {
					return serializeWithGson(object, contentType)
				}
				throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put either Jackson or Gson in the classpath.")
			} else if(containsIgnoreCase(ct, "xml")) {
				if(isJaxbPresent) {
					return serializeWithJaxb(object, contentType)
				} else {
					throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
				}
			} else {
				throw new IllegalArgumentException("Cannot serialize because cannot determine how to serialize content-type $contentType")
			}
		}

		return serializeWithJaxb(object, contentType)
	}

	private static String serializeWithObjectMapper(Object object, String contentType, ObjectMapperType mapperType) {
		if(mapperType == ObjectMapper.JACKSON && isJacksonPresent) {
			return serializeWithJackson(object, contentType)
		} else if(mapperType == ObjectMapper.GSON && isGsonPresent) {
			return serializeWithGson(object, contentType)
		} else if(mapperType == ObjectMapper.JAXB && isJaxbPresent) {
			return serializeWithJaxb(object, contentType)
		} else {
			def lowerCase = mapperType.toString().toLowerCase()
			throw new IllegalArgumentException("Cannot serialize object with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
		}
	}

	private static String serializeWithGson(Object object, String contentType) {
		new GsonMapper().serialize(object, contentType)
	}

	private static String serializeWithJackson(Object object, String contentType) {
		new JacksonMapper().serialize(object, contentType)
	}

	private static String serializeWithJaxb(object, String contentType) {
		new JaxbMapper().serialize(object, contentType)
	}

	private static def parseWithJaxb(Object object, String charset, Class cls) {
		new JaxbMapper().deserialize(object, cls)
	}

	private static def parseWithGson(object, String charset, Class cls) {
		new GsonMapper().deserialize(object, cls)
	}

	static def parseWithJackson(Object object, String charset, Class cls) {
		new JacksonMapper().deserialize(object, cls)
	}
}
