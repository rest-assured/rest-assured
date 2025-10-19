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

package io.restassured.internal.path.json;

import static io.restassured.internal.common.assertion.AssertionSupport.attributeGetter;
import static io.restassured.internal.common.assertion.AssertionSupport.classKeyword;
import static io.restassured.internal.common.assertion.AssertionSupport.escapePath;
import static io.restassured.internal.common.assertion.AssertionSupport.generateWhitespace;
import static io.restassured.internal.common.assertion.AssertionSupport.hyphen;
import static io.restassured.internal.common.assertion.AssertionSupport.integer;
import static io.restassured.internal.common.assertion.AssertionSupport.properties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import io.restassured.internal.common.assertion.Assertion;
import io.restassured.internal.common.assertion.PathFragmentEscaper;

public class JSONAssertion implements Assertion {

  private static final String ROOT = "restAssuredJsonRootObject";
  private static final String SCRIPT_NAME = "Script1";
  private static final Pattern KEY_PATTERN = Pattern.compile("^\\[-?\\d+].*");
	
  String key;
  Map<String, Object> params;

  @Override
  public Object getResult(Object object, Object config) {
    return getAsJsonObject(object);
  }

  @Override
  public String description() {
    return "JSON path";
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public Object getAsJsonObject(Object object) {
    key = (String) escapePath(
        key,
        (PathFragmentEscaper) hyphen(),
        (PathFragmentEscaper) attributeGetter(),
        (PathFragmentEscaper) integer(),
        (PathFragmentEscaper) properties(),
        (PathFragmentEscaper) classKeyword()
    );
    Object result;

    if ("$".equals(key) || key.isEmpty()) {
      result = object;
    } else {
      try {
        String expr;
        if (KEY_PATTERN.matcher(key).matches()) {
          expr = ROOT + key;
        } else {
          expr = ROOT + '.' + key;
        }
        result = eval(ROOT, object, expr);
      } catch (MissingPropertyException e) {
        String message = e.getMessage();
        // detect missed property on script-level. This should be defined by user as param
        if (message != null && (message.startsWith("No such property:") && message.endsWith("for class: " + SCRIPT_NAME))) {
          String error = String.format("The parameter \"%s\" was used but not defined. Define parameters using the JsonPath.param(...) function", e.getProperty());
          throw new IllegalArgumentException(error, e);
        }
        // return null if exception occurred for property from json path, see #1746
        return null;
      } catch (Exception e) {
        // Check if exception is due to a missing property
        if (e instanceof NullPointerException){
          String message = e.getMessage();
          if (message.equals("Cannot invoke method getAt() on null object") ||
             (message.startsWith("Cannot get property") && message.endsWith("on null object"))) {
            return null;
          }
        }
        String error = e.getMessage().replace("startup failed:", "Invalid JSON expression:").replace("$root.", generateWhitespace(ROOT.length()));
        throw new IllegalArgumentException(error, e);
      }
    }
    return result;
  }

  private Object eval(String root, Object object, String expr) {
    Map<String, Object> newParams;
    // Create parameters from given ones
    if (params != null) {
      newParams = new HashMap<>(params);
    } else {
      newParams = new HashMap<>(1);
    }
    // Add object to evaluate
    newParams.put(root, object);
    
	try (GroovyClassLoader loader = new GroovyClassLoader()) {
		Class<?> scriptClass = loader.parseClass(expr, SCRIPT_NAME + ".groovy");
		Script script = InvokerHelper.createScript(scriptClass, new Binding(newParams));
		return script.run();
	} catch (IOException e) {
		throw new UncheckedIOException(e);
	}
  }
}
