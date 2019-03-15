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

import io.restassured.internal.common.assertion.AssertParameter
import io.restassured.path.xml.element.Node
import io.restassured.path.xml.element.NodeChildren

class NodeImpl extends NodeBase implements Node {

  def attributes = [:]
  NodeChildren children = new NodeChildrenImpl()
  def name
  def value = null
  def groovyNode

  @Override
  Map<String, String> attributes() {
    return Collections.unmodifiableMap(attributes)
  }

  @Override
  String name() {
    return name
  }

  @Override
  Iterator<String> iterator() {
    if (!children.isEmpty()) {
      return children.iterator()
    } else {
      return new ValueIterator()
    }
  }

  String toString() {
    def builder = new StringBuilder()
    if (children.isEmpty()) {
      builder.append(value)
    } else {
      def iterator = children.iterator()
      while (iterator.hasNext()) {
        def next = iterator.next()
        builder.append(next)
      }
    }
    builder.toString()
  }

  /**
   * The the Node on the nth index
   *
   * @param index The index of the node the get
   * @return The node
   */
  Node get(int index) {
    return children.get(index)
  }

  NodeChildren children() {
    return children
  }

  def leftShift(Node node) {
    children << node
  }

  String getAttribute(String name) {
    AssertParameter.notNull(name, "name")
    if (!name.startsWith("@")) {
      name = "@" + name
    }
    return get(name)
  }

  float getFloat(String name) {
    return get(name)
  }

  double getDouble(String name) {
    return get(name)
  }

  char getChar(String name) {
    return get(name)
  }

  boolean getBoolean(String name) {
    return get(name)
  }

  long getLong(String name) {
    return get(name)
  }

  int getInt(String name) {
    return get(name)
  }

  short getShort(String name) {
    return get(name)
  }

  byte getByte(String name) {
    return get(name)
  }

  def get(String name) {
    if (name.startsWith("@")) {
      return attributes.get(name.substring(1))
    }
    return get(name, children.nodeIterator(), false)
  }

  @Override
  def getPath(String path) {
    new XMLAssertion(key: path).getChildResultAsJavaObject(groovyNode)
  }

  @Override
  String value() {
    return value
  }

  @Override
  def <T> List<T> getList(String name) {
    return get(name, children.nodeIterator(), true)
  }

  @Override
  Object getBackingGroovyObject() {
    return groovyNode
  }

  class ValueIterator implements Iterator<String> {
    def hasNext = true

    @Override
    boolean hasNext() {
      return hasNext
    }

    @Override
    String next() {
      hasNext = false
      return value
    }

    @Override
    void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
