/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.internal.path.xml

import com.jayway.restassured.path.Node
import com.jayway.restassured.path.NodeChildren

class NodeChildrenImpl extends NodeBase implements NodeChildren {
  def nodeList = []

  Node get(int index) {
    return nodeList.get(index)
  }

  int size() {
    return nodeList.size()
  }

  boolean isEmpty() {
    return nodeList.isEmpty()
  }

  Iterator<String> iterator() {
    return new NodeListIterator()
  }

  def leftShift(Node node) {
    nodeList << node
  }


  public String toString ( ) {
    def builder = new StringBuilder()
    nodeList.each {
      builder.append(it.toString())
    }
    builder.toString()
  }

  Iterable<Node> nodeIterable() {
    nodeList
  }

  public <T> T get(String name) {
    return get(name, nodeList.iterator())
  }

  @Override
  Iterator<Node> nodeIterator() {
    return nodeList.iterator()
  }

  @Override
  List<Node> list() {
    return Collections.unmodifiableList(nodeList)
  }

  class NodeListIterator implements Iterator<String> {
    def iterator = nodeList.iterator()
    @Override
    boolean hasNext() {
      return iterator.hasNext()
    }

    @Override
    String next() {
      def asString = iterator.next().toString()
      return asString
    }

    @Override
    void remove() {
    }
  }
}