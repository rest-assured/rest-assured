/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.path.jayway;

import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import io.restassured.common.config.NumberReturnType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Jayway JSON provider over plain maps and lists, as {@link JsonParser} creates them, so that Jayway doesn't need
 * json-smart to read a document.
 */
class MapsAndListsJsonProvider extends AbstractJsonProvider {
    @Override
    public Object parse(String json) throws InvalidJsonException {
        return JsonParser.parse(json, NumberReturnType.FLOAT_AND_DOUBLE, JsonParser.DEFAULT_NUMBER_LENGTH_LIMIT);
    }

    @Override
    public Object parse(InputStream jsonStream, String charset) throws InvalidJsonException {
        try {
            return JsonParser.parse(new InputStreamReader(jsonStream, charset), NumberReturnType.FLOAT_AND_DOUBLE, JsonParser.DEFAULT_NUMBER_LENGTH_LIMIT);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        StringBuilder json = new StringBuilder();
        write(obj, json);
        return json.toString();
    }

    @Override
    public Object createArray() {
        return new ArrayList<>();
    }

    @Override
    public Object createMap() {
        return new LinkedHashMap<String, Object>();
    }

    private static void write(Object value, StringBuilder json) {
        if (value instanceof Map<?, ?> map) {
            json.append('{');
            Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<?, ?> entry = entries.next();
                writeString(String.valueOf(entry.getKey()), json);
                json.append(':');
                write(entry.getValue(), json);
                if (entries.hasNext()) {
                    json.append(',');
                }
            }
            json.append('}');
        } else if (value instanceof List<?> list) {
            json.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                write(list.get(i), json);
            }
            json.append(']');
        } else if (value == null || value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else {
            writeString(value.toString(), json);
        }
    }

    private static void writeString(String string, StringBuilder json) {
        json.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
