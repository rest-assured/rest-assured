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

import io.restassured.mapper.ObjectMapperSerializationContext

class ObjectMapperSerializationContextImpl implements ObjectMapperSerializationContext {

  def object
  def contentType
  def charset

  @Override
  Object getObjectToSerialize() {
    return object
  }

  @Override
  def getObjectToSerializeAs(Class expectedType) {
    if (!expectedType.isAssignableFrom(object.getClass())) {
      throw new IllegalArgumentException("Object to serialize is not of required type $expectedType")
    }
    expectedType.cast(object)
  }

  @Override
  String getContentType() {
    return contentType
  }

  @Override
  String getCharset() {
    return charset
  }
}
