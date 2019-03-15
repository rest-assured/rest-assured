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
package io.restassured.module.spring.commons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class ParamLogger {

    private Map<String, Object> map;

    protected ParamLogger(Map<String, Object> parameters) {
        this.map = parameters;
    }

    public void logParams() {
        for (Map.Entry<String, Object> stringListEntry : map.entrySet()) {
            Object value = stringListEntry.getValue();
            Collection<Object> values;
            if (value instanceof Collection) {
                values = (Collection<Object>) value;
            } else {
                values = new ArrayList<Object>();
                values.add(value);
            }

            for (Object theValue : values) {
                logParam(stringListEntry.getKey(), theValue);
            }
        }
    }

    protected abstract void logParam(String paramName, Object paramValue);
}