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


package io.restassured.internal.mapping

import io.restassured.common.mapper.DataToDeserialize
import io.restassured.config.EncoderConfig
import io.restassured.config.ObjectMapperConfig
import io.restassured.http.ContentType
import io.restassured.internal.http.ContentTypeExtractor
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.mapper.ObjectMapperType
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory
import io.restassured.path.json.mapper.factory.Jackson1ObjectMapperFactory
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory
import io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory
import io.restassured.response.ResponseBodyData
import org.apache.commons.lang3.Validate

import java.lang.reflect.Type

import static io.restassured.common.mapper.resolver.ObjectMapperResolver.*
import static io.restassured.http.ContentType.ANY
import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase

class ObjectMapping {

  public
  static <T> T deserialize(ResponseBodyData response, Type cls, String contentType, String defaultContentType, String charset, ObjectMapperType mapperType,
                           ObjectMapperConfig objectMapperConfig) {
    Validate.notNull(objectMapperConfig, "String mapper configuration wasn't found, cannot deserialize.")
    def deserializationCtx = deserializationContext(response, cls, contentType, charset)
    if (objectMapperConfig.hasDefaultObjectMapper()) {
      return objectMapperConfig.defaultObjectMapper().deserialize(deserializationContext(response, cls, contentType, charset)) as T;
    } else if (mapperType != null || objectMapperConfig.hasDefaultObjectMapperType()) {
      ObjectMapperType mapperTypeToUse = mapperType == null ? objectMapperConfig.defaultObjectMapperType() : mapperType;
      return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, objectMapperConfig)
    }
    if (containsIgnoreCase(contentType, "json")) {
      if (isJackson2InClassPath()) {
        return parseWithJackson2(deserializationCtx, objectMapperConfig.jackson2ObjectMapperFactory()) as T
      } else if (isJackson1InClassPath()) {
        return parseWithJackson1(deserializationCtx, objectMapperConfig.jackson1ObjectMapperFactory()) as T
      } else if (isGsonInClassPath()) {
        return parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory()) as T
      } else if (isJohnzonInClassPath()) {
        return parseWithJohnzon(deserializationCtx, objectMapperConfig.johnzonObjectMapperFactory()) as T
      }
      throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson (Databind) or Gson in the classpath.")
    } else if (containsIgnoreCase(contentType, "xml")) {
      if (isJAXBInClassPath()) {
        return parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory()) as T
      }
      throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
    } else if (defaultContentType != null) {
      if (containsIgnoreCase(defaultContentType, "json")) {
        if (isJackson2InClassPath()) {
          return parseWithJackson2(deserializationCtx, objectMapperConfig.jackson2ObjectMapperFactory()) as T
        } else if (isJackson1InClassPath()) {
          return parseWithJackson1(deserializationCtx, objectMapperConfig.jackson1ObjectMapperFactory()) as T
        } else if (isGsonInClassPath()) {
          return parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory()) as T
		} else if (isJohnzonInClassPath()) {
			return parseWithJohnzon(deserializationCtx, objectMapperConfig.johnzonObjectMapperFactory()) as T
		}
      } else if (containsIgnoreCase(defaultContentType, "xml")) {
        if (isJAXBInClassPath()) {
          return parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory()) as T
        }
      }
    }
    throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was specified in response. Content-Type was '%s'.", contentType))
  }

  private
  static <T> T deserializeWithObjectMapper(ObjectMapperDeserializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
    if (mapperType == ObjectMapperType.JACKSON_2 && isJackson2InClassPath()) {
      return parseWithJackson2(ctx, config.jackson2ObjectMapperFactory()) as T
    } else if (mapperType == ObjectMapperType.JACKSON_1 && isJackson1InClassPath()) {
      return parseWithJackson1(ctx, config.jackson1ObjectMapperFactory()) as T
    } else if (mapperType == ObjectMapperType.GSON && isGsonInClassPath()) {
      return parseWithGson(ctx, config.gsonObjectMapperFactory()) as T
    } else if (mapperType == ObjectMapperType.JAXB && isJAXBInClassPath()) {
      return parseWithJaxb(ctx, config.jaxbObjectMapperFactory()) as T
    } else if (mapperType == ObjectMapperType.JOHNZON && isJohnzonInClassPath()) {
      return parseWithJohnzon(ctx, config.johnzonObjectMapperFactory()) as T
    } else {
      def lowerCase = mapperType.toString().toLowerCase()
      throw new IllegalArgumentException("Cannot map response body with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
    }
  }

  public static String serialize(Object object, String contentType, String charset, ObjectMapperType mapperType, ObjectMapperConfig config,
                                 EncoderConfig encoderConfig) {
    notNull(object, "String to serialize")
    notNull(config, "Object mapper configuration not found, cannot serialize object.")
    notNull(encoderConfig, "Encoder configuration not found, cannot serialize object.")

    def serializationCtx = serializationContext(object, contentType, charset)
    if (config.hasDefaultObjectMapper()) {
      return config.defaultObjectMapper().serialize(serializationContext(object, contentType, charset));
    } else if (mapperType != null || config.hasDefaultObjectMapperType()) {
      mapperType = mapperType ?: config.defaultObjectMapperType()
      return serializeWithObjectMapper(serializationCtx, mapperType, config)
    }

    if (contentType == null || contentType == ANY.toString()) {
      if (isJackson2InClassPath()) {
        return serializeWithJackson2(serializationCtx, config.jackson2ObjectMapperFactory())
      } else if (isJackson1InClassPath()) {
        return serializeWithJackson1(serializationCtx, config.jackson1ObjectMapperFactory())
      } else if (isGsonInClassPath()) {
        return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory())
      } else if (isJAXBInClassPath()) {
        return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
      } else if (isJohnzonInClassPath()) {
        return serializeWithJohnzon(serializationCtx, config.johnzonObjectMapperFactory())
      }
      throw new IllegalArgumentException("Cannot serialize because no JSON or XML serializer found in classpath.")
    } else {
      def ct = contentType.toLowerCase()
      if (containsIgnoreCase(ct, "json") || encoderConfig.contentEncoders().get(ContentTypeExtractor.getContentTypeWithoutCharset(ct)) == ContentType.JSON) {
        if (isJackson2InClassPath()) {
          return serializeWithJackson2(serializationCtx, config.jackson2ObjectMapperFactory())
        } else if (isJackson1InClassPath()) {
          return serializeWithJackson1(serializationCtx, config.jackson1ObjectMapperFactory())
        } else if (isGsonInClassPath()) {
          return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory())
        } else if (isJohnzonInClassPath()) {
          return serializeWithJohnzon(serializationCtx, config.johnzonObjectMapperFactory())
        }
        throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put either Jackson (Databind) or Gson in the classpath.")
      } else if (containsIgnoreCase(ct, "xml") || encoderConfig.contentEncoders().get(ContentTypeExtractor.getContentTypeWithoutCharset(ct)) == ContentType.XML) {
        if (isJAXBInClassPath()) {
          return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
        } else {
          throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
        }
      } else {
        def errorMessage = "Cannot serialize because cannot determine how to serialize content-type $contentType"

        def encoderType = encoderConfig.contentEncoders().get(ContentTypeExtractor.getContentTypeWithoutCharset(ct))
        if (encoderType) {
          errorMessage = errorMessage + " as ${encoderType.name()} (no serializer supports this format)"
        }
        throw new IllegalArgumentException(errorMessage)
      }
    }
    return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
  }

  private
  static String serializeWithObjectMapper(ObjectMapperSerializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
    if (mapperType == ObjectMapperType.JACKSON_2 && isJackson2InClassPath()) {
      return serializeWithJackson2(ctx, config.jackson2ObjectMapperFactory())
    } else if (mapperType == ObjectMapperType.JACKSON_1 && isJackson1InClassPath()) {
      return serializeWithJackson1(ctx, config.jackson1ObjectMapperFactory())
    } else if (mapperType == ObjectMapperType.GSON && isGsonInClassPath()) {
      return serializeWithGson(ctx, config.gsonObjectMapperFactory())
    } else if (mapperType == ObjectMapperType.JAXB && isJAXBInClassPath()) {
      return serializeWithJaxb(ctx, config.jaxbObjectMapperFactory())
    } else if (mapperType == ObjectMapperType.JOHNZON && isJohnzonInClassPath()) {
      return serializeWithJohnzon(ctx, config.johnzonObjectMapperFactory())
    } else {
      def lowerCase = mapperType.toString().toLowerCase()
      throw new IllegalArgumentException("Cannot serialize object with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
    }
  }

  private static String serializeWithGson(ObjectMapperSerializationContext ctx, GsonObjectMapperFactory factory) {
    new GsonMapper(factory).serialize(ctx)
  }

  private static String serializeWithJackson1(ObjectMapperSerializationContext ctx, Jackson1ObjectMapperFactory factory) {
    new Jackson1Mapper(factory).serialize(ctx)
  }

  private static String serializeWithJackson2(ObjectMapperSerializationContext ctx, Jackson2ObjectMapperFactory factory) {
    new Jackson2Mapper(factory).serialize(ctx)
  }


  private static String serializeWithJaxb(ObjectMapperSerializationContext ctx, JAXBObjectMapperFactory factory) {
    new JaxbMapper(factory).serialize(ctx)
  }

  private static String serializeWithJohnzon(ObjectMapperSerializationContext ctx, JohnzonObjectMapperFactory factory) {
    new JohnzonMapper(factory).serialize(ctx)
  }
	
  private static def parseWithJaxb(ObjectMapperDeserializationContext ctx, JAXBObjectMapperFactory factory) {
    new JaxbMapper(factory).deserialize(ctx)
  }

  private static def parseWithGson(ObjectMapperDeserializationContext ctx, GsonObjectMapperFactory factory) {
    new GsonMapper(factory).deserialize(ctx)
  }

  static def parseWithJackson1(ObjectMapperDeserializationContext ctx, Jackson1ObjectMapperFactory factory) {
    new Jackson1Mapper(factory).deserialize(ctx)
  }

  static def parseWithJackson2(ObjectMapperDeserializationContext ctx, Jackson2ObjectMapperFactory factory) {
    new Jackson2Mapper(factory).deserialize(ctx)
  }

  static def parseWithJohnzon(ObjectMapperDeserializationContext ctx, JohnzonObjectMapperFactory factory) {
    new JohnzonMapper(factory).deserialize(ctx)
  }
	
  private static ObjectMapperDeserializationContext deserializationContext(ResponseBodyData responseData, Type cls, contentType, charset) {
    def ctx = new ObjectMapperDeserializationContextImpl()
    ctx.type = cls
    ctx.charset = charset
    ctx.contentType = contentType
    ctx.dataToDeserialize = new DataToDeserialize() {
      @Override
      String asString() {
        return responseData.asString()
      }

      @Override
      byte[] asByteArray() {
        return responseData.asByteArray()
      }

      @Override
      InputStream asInputStream() {
        return responseData.asInputStream()
      }
    }
    ctx
  }

  private static ObjectMapperSerializationContext serializationContext(Object object, contentType, charset) {
    def ctx = new ObjectMapperSerializationContextImpl()
    ctx.charset = charset
    ctx.contentType = contentType
    ctx.object = object
    ctx
  }
}
