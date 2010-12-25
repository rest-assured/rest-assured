/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.specification;

public interface RequestSender {
  /**
   * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
   * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
   *
   * @param path The path to send the request to.
   */
  void get(String path);

  /**
   * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
   * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
   *
   * @param path The path to send the request to.
   */
  void post(String path);

  /**
   * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
   * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
   *
   * @param path The path to send the request to.
   */
  void put(String path);

  /**
   * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
   * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
   *
   * @param path The path to send the request to.
   */
  void delete(String path);

  /**
   * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
   * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
   *
   * @param path The path to send the request to.
   */
  void head(String path);
}