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



package io.restassured.internal.common.path

class ObjectConverter {
    def static <T> T convertObjectTo(Object object, Class<T> explicitType) {
        Object returnObject;
        if (object == null) {
            returnObject = null;
        } else if (!object.getClass().isAssignableFrom(explicitType)) {
            final String toString = object.toString();
            if (explicitType.isAssignableFrom(Integer.class) || explicitType.isAssignableFrom(int.class)) {
                returnObject = Integer.parseInt(toString);
            } else if (explicitType.isAssignableFrom(Boolean.class) || explicitType.isAssignableFrom(boolean.class)) {
                returnObject = Boolean.parseBoolean(toString);
            } else if (explicitType.isAssignableFrom(Character.class) || explicitType.isAssignableFrom(char.class)) {
                returnObject = toString.charAt(0);
            } else if (explicitType.isAssignableFrom(Byte.class) || explicitType.isAssignableFrom(byte.class)) {
                returnObject = Byte.parseByte(toString);
            } else if (explicitType.isAssignableFrom(Short.class) || explicitType.isAssignableFrom(short.class)) {
                returnObject = Short.parseShort(toString);
            } else if (explicitType.isAssignableFrom(Float.class) || explicitType.isAssignableFrom(float.class)) {
                returnObject = Float.parseFloat(toString);
            } else if (explicitType.isAssignableFrom(Double.class) || explicitType.isAssignableFrom(double.class)) {
                returnObject = Double.parseDouble(toString);
            } else if (explicitType.isAssignableFrom(Long.class) || explicitType.isAssignableFrom(long.class)) {
                returnObject = Long.parseLong(toString);
            } else if (explicitType.isAssignableFrom(BigDecimal.class)) {
                returnObject = new BigDecimal(toString);
            } else if (explicitType.isAssignableFrom(String.class)) {
                returnObject = toString;
            } else if (explicitType.isAssignableFrom(UUID.class)) {
                returnObject = UUID.fromString(toString);
            } else {
                try {
                    returnObject = explicitType.cast(object);
                } catch (ClassCastException e) {
                    throw new ClassCastException("Cannot convert " + object.getClass() + " to $explicitType.")
                }
            }
        } else {
            returnObject = explicitType.cast(object);
        }
        return (T) returnObject;
    }

    def static boolean canConvert(object, Class type) {
        try {
            convertObjectTo(object, type)
            true
        } catch (Exception e) {
            false
        }
    }
}
