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

package io.restassured.path.xml.element;

import java.util.Iterator;
import java.util.List;

/**
 * Represent the children of a Node in an XML document.
 */
public interface NodeChildren extends PathElement {

    /**
     * The the Node on the nth index
     *
     * @param index The index of the node the get
     * @return The node
     */
    Node get(int index);

    /**
     * @return The number of children
     */
    int size();

    /**
     * @return <code>true</code> if there are no children, <code>false</code> otherwise.
     */
    boolean isEmpty();

    /**
     * @return An iterable of all nodes
     */
    Iterable<Node> nodeIterable();

    /**
     * @return An iterator of all nodes.
     */
    Iterator<Node> nodeIterator();

    /**
     * @return The child nodes as a list
     */
    List<Node> list();
}
