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

import io.restassured.common.mapper.ObjectDeserializationContext
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory
import io.restassured.path.xml.mapping.XmlPathObjectDeserializer

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.transform.stream.StreamSource

class XmlPathJaxbObjectDeserializer implements XmlPathObjectDeserializer {

  private final JAXBObjectMapperFactory factory

  XmlPathJaxbObjectDeserializer(JAXBObjectMapperFactory factory) {
    this.factory = factory
  }

  @Override
  def deserialize(ObjectDeserializationContext context) {
    def cls = context.getType()
    def object = context.getDataToDeserialize().asString()
    JAXBContext jaxbContext = factory.create(cls, context.getCharset())

    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller()
    def reader = new StringReader(object)
    if (cls.isAnnotationPresent(XmlRootElement.class)) {
      unmarshaller.unmarshal(reader)
    } else {
      JAXBElement jaxbElement = unmarshaller.unmarshal(new StreamSource(reader), cls)
      jaxbElement.getValue()
    }
  }
}
