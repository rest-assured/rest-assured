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


package io.restassured.internal.path.xml;

import io.restassured.path.xml.element.Node;
import io.restassured.path.xml.element.NodeChildren;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class NodeChildrenImpl extends NodeBase implements NodeChildren {
    public List<Node> nodeList = new ArrayList<>();
    public Object groovyNodes;

    @Override
    public Node get(int index) {
        return nodeList.get(index);
    }

    @Override
    public int size() {
        return nodeList.size();
    }

    @Override
    public boolean isEmpty() {
        return nodeList.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return new NodeListIterator(nodeList.iterator());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        nodeList.forEach(it -> builder.append(it.toString()));
        return builder.toString();
    }

    @Override
    public Iterable<Node> nodeIterable() {
        return nodeList;
    }

    @Override
    public Object get(String name) {
        return get(name, nodeList.iterator(), false);
    }

    @Override
    public Object getPath(String path) {
        XMLAssertion xmlAssertion = new XMLAssertion();
        xmlAssertion.setKey(path);
        return xmlAssertion.getChildResultAsJavaObject(groovyNodes);
    }

    @Override
    public Iterator<Node> nodeIterator() {
        return nodeList.iterator();
    }

    @Override
    public List<Node> list() {
        return Collections.unmodifiableList(nodeList);
    }

    @Override
    public <T> List<T> getList(String name) {
        return get(name, nodeList.iterator(), true);
    }

    @Override
    public Object getBackingGroovyObject() {
        return groovyNodes;
    }

    @SuppressWarnings("unused")
    Object leftShift(Node node) {
        nodeList.add(node);
        return nodeList;
    }

    static class NodeListIterator implements Iterator<String> {
        private final Iterator<Node> iterator;

        public NodeListIterator(Iterator<Node> nodeList) {
            this.iterator = nodeList;
        }


        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            return iterator.next().toString();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}