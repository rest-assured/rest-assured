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

package io.restassured.internal.support;

import io.restassured.config.ParamConfig.UpdateStrategy;
import io.restassured.internal.NoParameterValue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static java.util.Arrays.asList;

public class ParameterUpdater {
    private final Serializer serializer;

    public ParameterUpdater(Serializer serializer) {
        this.serializer = serializer;
    }

    public void updateParameters(UpdateStrategy strategy, Map<String, Object> from, Map<String, Object> to) {
        notNull(from, "Map to copy from");
        notNull(to, "Map to copy to");
        notNull(strategy, UpdateStrategy.class);
        // Groovy callers can pass maps whose keys aren't Strings. Groovy passed a GString key as a String (and failed for
        // any other key that isn't a String), so use the string form of the key, like the header expectations do.
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) from).entrySet()) {
            String key = castToString(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Collection) {
                @SuppressWarnings("unchecked") Collection<Object> values = (Collection<Object>) value;
                updateCollectionParameter(strategy, to, key, values);
            } else {
                updateStandardParameter(strategy, to, key, value);
            }
        }
    }

    public void updateCollectionParameter(UpdateStrategy strategy, Map<String, Object> to, String key, Collection<Object> values) {
        if (values == null || values.isEmpty()) {
            to.put(key, new NoParameterValue());
            return;
        }

        List<Object> convertedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            convertedValues.add(serializer.serializeIfNeeded(value));
        }
        if (strategy == MERGE) {
            if (to.containsKey(key)) {
                Object currentValue = to.get(key);
                if (currentValue instanceof Collection) {
                    @SuppressWarnings("unchecked") Collection<Object> currentValues = (Collection<Object>) currentValue;
                    currentValues.addAll(convertedValues);
                } else {
                    to.put(key, flatten(currentValue, convertedValues));
                }
            } else {
                to.put(key, new LinkedList<>(convertedValues));
            }
        } else {
            to.put(key, convertedValues);
        }
    }

    public void updateZeroToManyParameters(UpdateStrategy strategy, Map<String, Object> to, String parameterName, Object... parameterValues) {
        if (isEmpty(parameterValues)) {
            updateStandardParameter(strategy, to, parameterName);
        } else if (parameterValues.length == 1) {
            updateStandardParameter(strategy, to, parameterName, parameterValues[0]);
        } else {
            updateCollectionParameter(strategy, to, parameterName, asList(parameterValues));
        }
    }

    public void updateStandardParameter(UpdateStrategy strategy, Map<String, Object> to, String key) {
        updateStandardParameter(strategy, to, key, null);
    }

    public void updateStandardParameter(UpdateStrategy strategy, Map<String, Object> to, String key, Object value) {
        if (value == null) {
            to.put(key, new NoParameterValue());
            return;
        }
        Object newValue = serializer.serializeIfNeeded(value);
        if (strategy == MERGE) {
            if (to.containsKey(key)) {
                Object currentValue = to.get(key);
                if (currentValue instanceof List) {
                    @SuppressWarnings("unchecked") List<Object> currentValues = (List<Object>) currentValue;
                    currentValues.add(newValue);
                } else {
                    to.put(key, new ArrayList<>(asList(currentValue, newValue)));
                }
            } else {
                to.put(key, newValue);
            }
        } else {
            to.put(key, newValue);
        }
    }

    private static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length == 0 || (objects.length == 1 && objects[0] instanceof NoParameterValue);
    }

    /**
     * Works like Groovy's {@code [currentValue, values].flatten()}: iterators, collections and arrays are flattened
     * recursively, a present Optional is replaced by its value, an empty Optional is left out.
     */
    private static List<Object> flatten(Object currentValue, List<Object> values) {
        List<Object> flattened = new ArrayList<>();
        flattenInto(currentValue, flattened);
        flattenInto(values, flattened);
        return flattened;
    }

    private static void flattenInto(Object element, List<Object> flattened) {
        if (element instanceof Iterator) {
            Iterator<?> iterator = (Iterator<?>) element;
            while (iterator.hasNext()) {
                flattenInto(iterator.next(), flattened);
            }
        } else if (element instanceof Collection) {
            for (Object item : (Collection<?>) element) {
                flattenInto(item, flattened);
            }
        } else if (element != null && element.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(element); i++) {
                flattenInto(Array.get(element, i), flattened);
            }
        } else if (element instanceof Optional) {
            ((Optional<?>) element).ifPresent(flattened::add);
        } else {
            flattened.add(element);
        }
    }

    public interface Serializer {
        String serializeIfNeeded(Object value);
    }
}
