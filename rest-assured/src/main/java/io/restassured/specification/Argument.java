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

package io.restassured.specification;

/**
 * An argument that can be used to build up a body path expression.
 */
public class Argument {

    private final Object argument;

    /**
     * The argument, can be null.
     *
     * @param argument The argument.
     */
    public Argument(Object argument) {
        this.argument = argument;
    }

    public Object getArgument() {
        return argument;
    }

    public static Argument withArg(Object arg) {
        return new Argument(arg);
    }

    public static Argument arg(Object arg) {
        return new Argument(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Argument argument1 = (Argument) o;

        if (argument != null ? !argument.equals(argument1.argument) : argument1.argument != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return argument != null ? argument.hashCode() : 0;
    }
}
