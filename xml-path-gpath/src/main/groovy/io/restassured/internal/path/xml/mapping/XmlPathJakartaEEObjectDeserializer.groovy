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
import io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory
import io.restassured.path.xml.mapping.XmlPathObjectDeserializer

class XmlPathJakartaEEObjectDeserializer implements XmlPathObjectDeserializer {

  private final JakartaEEObjectMapperFactory factory

  XmlPathJakartaEEObjectDeserializer(JakartaEEObjectMapperFactory factory) {
    this.factory = factory
  }

  @SuppressWarnings('UnnecessaryQualifiedReference')
  @Override
  def deserialize(ObjectDeserializationContext context) {
    def cls = context.getType()
    def object = context.getDataToDeserialize().asString()
    jakarta.xml.bind.JAXBContext jaxbContext = factory.create(cls, context.getCharset())

    jakarta.xml.bind.Unmarshaller unmarshaller = jaxbContext.createUnmarshaller()
    def reader = new StringReader(object)
    if (cls.isAnnotationPresent(jakarta.xml.bind.annotation.XmlRootElement.class)) {
      unmarshaller.unmarshal(reader)
    } else {
      jakarta.xml.bind.JAXBElement jaxbElement = unmarshaller.unmarshal(new javax.xml.transform.stream.StreamSource(reader), cls)
      jaxbElement.getValue()
    }
  }
}
