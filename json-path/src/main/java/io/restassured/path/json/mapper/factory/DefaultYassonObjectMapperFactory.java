/*
 * Copyright 2020 the original author or authors.
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
package io.restassured.path.json.mapper.factory;

import jakarta.json.bind.Jsonb;
import org.eclipse.yasson.JsonBindingProvider;

import java.lang.reflect.Type;

public class DefaultYassonObjectMapperFactory implements JsonbObjectMapperFactory {

    // Double-checked locking on a volatile so that threads racing the first call share one instance
    private static volatile Jsonb cachedJsonb = null;

    @Override
    public Jsonb create(Type cls, String charset) {
        Jsonb jsonb = cachedJsonb;
        if (jsonb == null) {
            synchronized (DefaultYassonObjectMapperFactory.class) {
                jsonb = cachedJsonb;
                if (jsonb == null) {
                    jsonb = new JsonBindingProvider().create().build();
                    cachedJsonb = jsonb;
                }
            }
        }
        return jsonb;
    }
}
