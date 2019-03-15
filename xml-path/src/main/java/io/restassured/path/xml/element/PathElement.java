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

import java.util.List;

/**
 * Base object for all XML objects.
 */
public interface PathElement extends Iterable<String> {

    /**
     * Get a value from the current XML object.
     * <p>
     * This method returns the child whose name matches <code>name</code>. If several
     * children matches the name then a {@link List} of Node's
     * are returned.
     * </p>
     * <p>
     * If this object is a Node and you want to return an attribute value you need to prefix
     * the name with an <tt>@</tt>. E.g. given
     * <pre>
     *   &lt;category type=&quot;present&quot;&gt;
     *      &lt;item when=&quot;Aug 10&quot;&gt;
     *         &lt;name&gt;Kathryn&#39;s Birthday&lt;/name&gt;
     *         &lt;price&gt;200&lt;/price&gt;
     *      &lt;/item&gt;
     *   &lt;/category&gt;
     * </pre>
     * then
     * <pre>
     *  String type = node.get("@type");
     * </pre>
     * will return "present".
     * </p>
     *
     * @param name The name of the child, children or attribute.
     * @param <T>  The expected type of the return value.
     * @return A Node, a list of nodes, an attribute value or null if not found.
     */
    <T> T get(String name);

    /**
     * Get a value from the current XML using Groovy's <a href="http://docs.groovy-lang.org/latest/html/documentation/#_gpath">GPath</a> expression syntax.
     *
     * @param path The GPath expression syntax
     * @param <T>  The expected type of the return value.
     * @return The outcome of the gpath expression
     */
    <T> T getPath(String path);

    /**
     * Get a value from the current XML using Groovy's <a href="http://docs.groovy-lang.org/latest/html/documentation/#_gpath">GPath</a> expression syntax.
     *
     * @param path         The GPath expression syntax
     * @param explicitType Converts the outcome of the GPath expression to tis type if allowed
     * @param <T>          The expected type of the return value.
     * @return The outcome of the gpath expression
     */
    <T> T getPath(String path, Class<T> explicitType);

    /**
     * Get a {@link Node} whose name matches the supplied <code>name</code>
     * from the current XML object.
     * <p/>
     * <p/>
     * Note that there's no guarantee that this method actually
     * will return a Node. It could result in a {@java.lang.ClassCastException}.
     *
     * @param name The name of the Node to get.
     * @return The Node matching the name or <code>null</code> if non were found.
     */
    Node getNode(String name);

    /**
     * Get a list of {@link Node}'s whose name matches the supplied <code>name</code>
     * from the current XML object.
     *
     * @param name The name of the Nodes to get.
     * @return The Node matching the name or an empty list of non were found.
     */
    List<Node> getNodes(String name);
}
