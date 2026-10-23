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

package io.restassured.internal.common.spi.path;

/**
 * The result of evaluating a path. A provider that can't tell a missing property from one whose value is {@code null},
 * such as GPath, always returns {@link #value(Object)}.
 *
 * @param kind    What the path found.
 * @param value   The value, for {@link Kind#VALUE}.
 * @param message Why the path can't be evaluated, for {@link Kind#LIST_ADDRESSED_AS_OBJECT}.
 */
public record PathResult(Kind kind, Object value, String message) {

    public enum Kind {
        /**
         * The path found a value, which may be {@code null}.
         */
        VALUE,
        /**
         * The path addresses a property, or a parent of one, that the document doesn't have. REST Assured treats it as {@code null}.
         */
        MISSING,
        /**
         * The path addresses a property of a list as if the list were an object. REST Assured fails with the message.
         */
        LIST_ADDRESSED_AS_OBJECT
    }

    private static final PathResult MISSING_RESULT = new PathResult(Kind.MISSING, null, null);

    public static PathResult value(Object value) {
        return new PathResult(Kind.VALUE, value, null);
    }

    public static PathResult missing() {
        return MISSING_RESULT;
    }

    public static PathResult listAddressedAsObject(String message) {
        return new PathResult(Kind.LIST_ADDRESSED_AS_OBJECT, null, message);
    }
}
