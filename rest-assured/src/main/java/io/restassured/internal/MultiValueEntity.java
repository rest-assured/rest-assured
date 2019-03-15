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

package io.restassured.internal;

import java.util.*;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class MultiValueEntity<T extends NameAndValue> implements Iterable<T> {

    private final List<T> entities;

    public MultiValueEntity(List<T> entities) {
        notNull(entities, "Entities");
        this.entities = entities;
    }

    /**
     * @return The size of the entities
     */
    public int size() {
        return entities.size();
    }

    /**
     * @return <code>true</code> if one or more entities are defined, <code>false</code> otherwise.
     */
    public boolean exist() {
        return !entities.isEmpty();
    }

    /**                                               C
     * See if a entity with the given name exists
     *
     * @param entityName The name of the entity to check
     * @return <code>true</code> if the entity exists
     */
    public boolean hasEntityWithName(String entityName) {
        return get(entityName) != null;
    }

    public List<T> list() {
        return Collections.unmodifiableList(entities);
    }

    /**
     *  Get a single entity with the supplied name. If there are several entities match the <code>entityName</code> then
     *  the last one is returned.
     *
     * @param entityName The name of the entity to find
     * @return The found entity or <code>null</code> if no entity was found.
     */
    public T get(String entityName) {
        notNull(entityName, "Entity name");
        List<T> copyOfEntities = reverse();
        for (T entity : copyOfEntities) {
            if(entity.getName().equalsIgnoreCase(entityName)) {
                return entity;
            }
        }
        return null;
    }

    private List<T> reverse() {
        List<T> copy = new ArrayList<T>(entities);
        Collections.reverse(copy);
        return copy;
    }

    /**
     *  Get a single entity value with the supplied name. If there are several headers match the <code>headerName</code> then
     *  the last one is returned.
     *
     * @param entityName The name of the header to find
     * @return The found entity value or <code>null</code> if no header was found.
     */
    public String getValue(String entityName) {
        notNull(entityName, "Entity name");
        final T entity = get(entityName);
        if(entity == null) {
            return null;
        }
        return entity.getValue();
    }


    /**
     *  Get all entities with the supplied name. If there's only one entity matching the <code>entityName</code> then
     *  a list with only that entity is returned.
     *
     * @param entityName The name of the entity to find
     * @return The found entities or empty list if no entity was found.
     */
    public List<T> getList(String entityName) {
        notNull(entityName, "Entity name");
        final List<T> entityList = new ArrayList<T>();
        for (T entity : entities) {
            if(entity.getName().equalsIgnoreCase(entityName)) {
                entityList.add(entity);
            }
        }
        return Collections.unmodifiableList(entityList);
    }

    /**
     *  Get all entity values of the entity with supplied name. If there's only one header matching the <code>entity name</code> then
     *  a list with only that header value is returned.
     *
     * @param entityName The name of the entity to find
     * @return The found entity values or empty list if no entity was found.
     */
    public List<String> getValues(String entityName) {
        final List<T> list = getList(entityName);
        final List<String> stringList = new LinkedList<String>();
        for (T entity : list) {
            stringList.add(entity.getValue());
        }
        return Collections.unmodifiableList(stringList);
    }

    public Iterator<T> iterator() {
        return entities.iterator();
    }

    public List<T> asList() {
        return Collections.unmodifiableList(entities);
    }

    @Override
    public String toString() {
        if(!exist()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (T entity : entities) {
            builder.append(entity).append("\n");
        }
        builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }
}
