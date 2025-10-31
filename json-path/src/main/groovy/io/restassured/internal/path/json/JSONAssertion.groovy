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

package io.restassured.internal.path.json

import io.restassured.internal.common.assertion.Assertion

import static io.restassured.internal.common.assertion.AssertionSupport.*

class JSONAssertion implements Assertion {
  String key
  Map<String, Object> params

  Object getResult(object, config) {
    Object result = getAsJsonObject(object)
    return result
  }

  def getAsJsonObject(object) {
    key = escapePath(key, hyphen(), attributeGetter(), integer(), properties(), classKeyword())
    def result
    if (key == "\$" || key == "") {
      result = object
    } else {
      def root = 'restAssuredJsonRootObject'
      try {
        def expr
        if (key =~ /^\[-?\d+].*/) {
          expr = "$root$key"
        } else {
          expr = "$root.$key"
        }
        result = eval(root, object, expr)
      } catch (MissingPropertyException e) {
        def message = e.getMessage();
        // detect missed property on script-level. This should be defined by user as param
        if (message != null && (message.startsWith("No such property:") && message.endsWith("for class: Script1"))) {
          String error = String.format("The parameter \"%s\" was used but not defined. Define parameters using the JsonPath.param(...) function", e.property)
          throw new IllegalArgumentException(error, e)
        }
        // return null if exception occurred for property from json path, see #1746
        return null
      } catch (Exception e) {
        // Check if exception is due to a missing property
        if (e instanceof NullPointerException){
          def message = e.getMessage();
          if (message.equals("Cannot invoke method getAt() on null object") ||
             (message.startsWith("Cannot get property") && message.endsWith("on null object"))) {
            return null
          }
        }
        String error = e.getMessage().replace("startup failed:", "Invalid JSON expression:").replace("$root.", generateWhitespace(root.length()))
        throw new IllegalArgumentException(error, e)
      }
    }
    return result
  }

  String description() {
    return "JSON path"
  }

  private def eval(root, object, expr) {
    Map<String, Object> newParams
    // Create parameters from given ones
    if (params != null) {
      newParams = new HashMap<>(params)
    } else {
      newParams = new HashMap<>()
    }
    // Add object to evaluate
    newParams.put(root, object)
    // Create shell with variables set
    GroovyShell sh = new GroovyShell(new Binding(newParams))
    // Run
    def res = sh.evaluate(expr)
    sh.resetLoadedClasses()
    return res
  }
}
