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

package io.restassured.internal.path.xml.mapping

import io.restassured.common.mapper.DataToDeserialize
import io.restassured.common.mapper.ObjectDeserializationContext
import io.restassured.internal.common.mapper.ObjectDeserializationContextImpl
import io.restassured.path.xml.config.XmlParserType
import io.restassured.path.xml.config.XmlPathConfig
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory
import org.apache.commons.lang3.Validate

import static io.restassured.common.mapper.resolver.ObjectMapperResolver.isJAXBInClassPath

class XmlObjectDeserializer {

  static <T> T deserialize(String xml, Class<T> cls, XmlPathConfig xmlPathConfig) {
    Validate.notNull(xmlPathConfig, "XmlPath configuration wasn't specified, cannot deserialize.")
    def deserializationCtx = new ObjectDeserializationContextImpl()
    def mapperType = xmlPathConfig.defaultParserType()
    deserializationCtx.type = cls
    deserializationCtx.charset = xmlPathConfig.charset()
    deserializationCtx.dataToDeserialize = new DataToDeserialize() {

      @Override
      String asString() {
        return xml
      }

      @Override
      byte[] asByteArray() {
        return xml.getBytes(xmlPathConfig.charset())
      }

      @Override
      InputStream asInputStream() {
        return new ByteArrayInputStream(asByteArray())
      }
    }

    if (xmlPathConfig.hasDefaultDeserializer()) {
      return xmlPathConfig.defaultDeserializer().deserialize(deserializationCtx) as T;
    } else if (mapperType != null || xmlPathConfig.hasDefaultParserType()) {
      XmlParserType mapperTypeToUse = mapperType == null ? xmlPathConfig.defaultParserType() : mapperType;
      return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, xmlPathConfig)
    }

    if (isJAXBInClassPath()) {
      return deserializeWithJaxb(deserializationCtx, xmlPathConfig.jaxbObjectMapperFactory()) as T
    }

    throw new IllegalStateException("Cannot deserialize object because no XML deserializer found in classpath. Please put JAXB in the classpath.")
  }

  private static <T> T deserializeWithObjectMapper(ObjectDeserializationContext ctx, XmlParserType mapperType, XmlPathConfig config) {
    if (mapperType == XmlParserType.JAXB && isJAXBInClassPath()) {
      return deserializeWithJaxb(ctx, config.jaxbObjectMapperFactory()) as T
    } else {
      def lowerCase = mapperType.toString().toLowerCase()
      throw new IllegalArgumentException("Cannot deserialize object using $mapperType because $lowerCase doesn't exist in the classpath.")
    }
  }

  static def deserializeWithJaxb(ObjectDeserializationContext ctx, JAXBObjectMapperFactory factory) {
    new XmlPathJaxbObjectDeserializer(factory).deserialize(ctx)
  }

}
