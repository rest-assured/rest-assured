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

package io.restassured.internal.proxy

import io.restassured.specification.ProxySpecification

import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static java.net.Proxy.Type.HTTP

/**
 * Proxy selector implementation that uses {@link ProxySpecification} to determine the Proxy to connect to.
 * If no ProxySpecification is defined then it delegates to the <code>delegatingProxySelector</code>.
 */
class RestAssuredProxySelector extends ProxySelector {
  def ProxySelector delegatingProxySelector
  def ProxySpecification proxySpecification

  List<Proxy> select(URI uri) {
    notNull(uri, URI.class);
    def proxies
    if (proxySpecification) {
      proxies = [new Proxy(HTTP, new InetSocketAddress(proxySpecification.host, proxySpecification.port))]
    } else {
      proxies = delegatingProxySelector.select(uri)
    }
    proxies
  }

  void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    if (proxySpecification) {
      throw ioe;
    } else {
      delegatingProxySelector.connectFailed(uri, sa, ioe)
    }
  }
}