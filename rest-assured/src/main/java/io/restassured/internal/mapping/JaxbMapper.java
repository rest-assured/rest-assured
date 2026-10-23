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

package io.restassured.internal.mapping;

import io.restassured.common.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class JaxbMapper implements ObjectMapper {

    private final JAXBObjectMapperFactory factory;

    public JaxbMapper(JAXBObjectMapperFactory factory) {
        this.factory = factory;
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        Class<?> cls = MapperSupport.classToUnmarshal(context.getType());
        StringReader reader = new StringReader(context.getDataToDeserialize().asString());
        JAXBContext jaxbContext = factory.create(cls, context.getCharset());
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            if (cls.isAnnotationPresent(XmlRootElement.class)) {
                return unmarshaller.unmarshal(reader);
            }
            return unmarshaller.unmarshal(new StreamSource(reader), cls).getValue();
        } catch (JAXBException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        String charset = context.getCharset();
        JAXBContext jaxbContext = factory.create(MapperSupport.typeOf(object), charset);
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            if (charset != null) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, charset);
            }
            StringWriter sw = new StringWriter();
            marshaller.marshal(object, sw);
            return sw.toString();
        } catch (JAXBException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }
}
