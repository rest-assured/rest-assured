/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.internal.path.xml

import com.jayway.restassured.path.Node

abstract class NodeBase {

  abstract <T> T get(String name)

  public <T> T get(String name, iterator) {
    def found = []
    while(iterator.hasNext()) {
      def next = iterator.next();
      if(next.name() == name) {
        found << next
      }
    }
    found.size() == 1 ? found.get(0) : Collections.unmodifiableList(found)
  }

  String getString(String name) {
    return get(name)
  }
}
