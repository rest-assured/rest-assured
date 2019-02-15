/*
 * Copyright 2016 the original author or authors.
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

import io.restassured.path.xml.element.Node
import io.restassured.internal.common.path.ObjectConverter

abstract class NodeBase {

    abstract <T> T get(String name)

    abstract <T> List<T> getList(String name)

    protected <T> T get(String name, iterator, boolean forceList) {
        def found = []
        while (iterator.hasNext()) {
            def next = iterator.next();
            if (next.name() == name) {
                found << next
            }
        }
        if (forceList) {
            Collections.unmodifiableList(found)
        } else if (found.size() == 1) {
            found.get(0)
        } else if (found.isEmpty()) {
            null
        } else {
            Collections.unmodifiableList(found)
        }
    }

    def abstract <T> T getPath(String path)

    def <T> T getPath(String path, Class<T> explicitType) {
        def object = getPath(path)
        return ObjectConverter.convertObjectTo(object, explicitType)
    }


    public Node getNode(String name) {
        return get(name)
    }

    public List<Node> getNodes(String name) {
        return getList(name)
    }

    public abstract Object getBackingGroovyObject();

}