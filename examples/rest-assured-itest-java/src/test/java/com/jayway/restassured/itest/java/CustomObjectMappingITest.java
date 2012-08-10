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
package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.objects.Message;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext;
import com.jayway.restassured.mapper.ObjectMapperSerializationContext;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CustomObjectMappingITest extends WithJetty {

    @Test
    public void ikk() throws Exception {
        final Message message = new Message();
        message.setMessage("A message");
        final ObjectMapper mapper = new ObjectMapper() {
            public Object deserialize(ObjectMapperDeserializationContext context) {
                final String toDeserialize = context.getResponse().asString();
                final String unquoted = StringUtils.remove(toDeserialize, "#");
                final Message message = new Message();
                message.setMessage(unquoted);
                return message;
            }

            public Object serialize(ObjectMapperSerializationContext context) {
                final Message objectToSerialize = context.getObjectToSerializeAs(Message.class);
                final String message = objectToSerialize.getMessage();
                return "##" + message + "##";
            }
        };

        final Message returnedMessage = given().body(message, mapper).when().post("/reflect").as(Message.class, mapper);

        assertThat(returnedMessage.getMessage(), equalTo("A message"));
    }
}
