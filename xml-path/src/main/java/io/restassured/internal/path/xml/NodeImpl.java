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

import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.path.xml.element.Node;
import io.restassured.path.xml.element.NodeChildren;

import java.util.*;

class NodeImpl extends NodeBase implements Node {

    Map<String, String> attributes = new HashMap<>();
    NodeChildrenImpl children = new NodeChildrenImpl();
    String name;
    String value = null;
    Object groovyNode;

    @Override
    public Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Iterator<String> iterator() {
        if (!children.isEmpty()) {
            return children.iterator();
        } else {
            return new ValueIterator();
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (children.isEmpty()) {
            builder.append(value);
        } else {
            for (Object next : children) {
                builder.append(next);
            }
        }
        return builder.toString();
    }

    /**
     * The the Node on the nth index
     *
     * @param index The index of the node the get
     * @return The node
     */
    public Node get(int index) {
        return children.get(index);
    }

    public NodeChildren children() {
        return children;
    }

    public String getAttribute(String name) {
        AssertParameter.notNull(name, "name");
        if (!name.startsWith("@")) {
            name = "@" + name;
        }
        return (String) get(name);
    }

    float getFloat(String name) {
        return (float) get(name);
    }

    double getDouble(String name) {
        return (double) get(name);
    }

    char getChar(String name) {
        return (char) get(name);
    }

    boolean getBoolean(String name) {
        return (boolean) get(name);
    }

    long getLong(String name) {
        return (long) get(name);
    }

    int getInt(String name) {
        return (int) get(name);
    }

    short getShort(String name) {
        return (short) get(name);
    }

    byte getByte(String name) {
        return (byte) get(name);
    }

    @Override
    public Object get(String name) {
        if (name.startsWith("@")) {
            return attributes.get(name.substring(1));
        }
        return get(name, children.nodeIterator(), false);
    }

    @Override
    public Object getPath(String path) {
        XMLAssertion xmlAssertion = new XMLAssertion();
        xmlAssertion.setKey(path);
        return xmlAssertion.getChildResultAsJavaObject(groovyNode);
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public <T> List<T> getList(String name) {
        return get(name, children.nodeIterator(), true);
    }

    @Override
    public Object getBackingGroovyObject() {
        return groovyNode;
    }

    @SuppressWarnings("unused")
    Object leftShift(Node node) {
        return children.leftShift(node);
    }

    class ValueIterator implements Iterator<String> {
        boolean hasNext = true;

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public String next() {
            hasNext = false;
            return value;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
