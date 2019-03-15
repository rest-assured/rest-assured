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

import java.util.Map;

/**
 * A Node represents a single node in a XML document.
 */
public interface Node extends PathElement {
    /**
     * The node attributes, may be empty.
     *
     * @return The attributes associated with this node.
     */
    Map<String, String> attributes();

    /**
     * The node children
     *
     * @return The node children
     */
    NodeChildren children();

    /**
     * The name of the node
     *
     * @return The name of the node
     */
    String name();

    /**
     * The node value. Will return null unless this node is a leaf node in the XML document.
     *
     * @return The node value.
     */
    String value();

    /**
     * Get the value of an attribute.
     *
     * @param name The name of the attribute to get
     * @return The value of the attribute or null if no attribute with the given name was found.
     */
    String getAttribute(String name);
}
