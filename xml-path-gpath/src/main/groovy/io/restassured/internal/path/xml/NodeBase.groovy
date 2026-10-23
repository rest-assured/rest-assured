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

package io.restassured.internal.path.xml

import io.restassured.internal.common.path.ObjectConverter
import io.restassured.path.xml.element.Node
import io.restassured.path.xml.element.PathElement

abstract class NodeBase implements PathElement {

  @Override
  abstract get(String name)

  abstract <T> List<T> getList(String name)

  protected static <T> T get(String name, iterator, boolean forceList) {
    def found = []
    while (iterator.hasNext()) {
      def next = iterator.next()
      if (next.name() == name) {
        found << next
      }
    }
    if (forceList) {
      Collections.unmodifiableList(found) as T
    } else if (found.size() == 1) {
      found.get(0)
    } else if (found.isEmpty()) {
      null
    } else {
      Collections.unmodifiableList(found) as T
    }
  }

  abstract getPath(String path)

  @Override
  def getPath(String path, Class explicitType) {
    def object = getPath(path)
    ObjectConverter.convertObjectTo(object, explicitType)
  }

  Node getNode(String name) {
    return get(name)
  }

  List<Node> getNodes(String name) {
    return getList(name)
  }

  abstract Object getBackingGroovyObject();

}