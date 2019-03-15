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

package io.restassured.itest.java;

import io.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.specification.ProxySpecification.auth;
import static io.restassured.specification.ProxySpecification.host;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

public class ProxyAuthITest extends WithJetty {

    static HttpProxyServer proxyServer;

    @BeforeClass public static void
    create_proxy_server() {
        proxyServer = DefaultHttpProxyServer.bootstrap().withPort(8888).withAllowLocalOnly(true).
                withProxyAuthenticator((userName, password) -> "admin".equals(userName) && "pass".equals(password)).start();
    }

    @AfterClass public static void
    stop_proxy_server() {
        proxyServer.stop();
        proxyServer = null;
        FileUtils.deleteQuietly(new File("littleproxy_cert"));
        FileUtils.deleteQuietly(new File("littleproxy_keystore.jks"));
    }

    @Test public void
    using_proxy_with_host_port_and_auth() {
        given().
                proxy(host("127.0.0.1").withPort(8888).withAuth("admin", "pass")).
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greetJSON").
        then().
                header("Via", not(emptyOrNullString()));
    }

    @Test public void
    using_proxy_with_auth() {
        given().
                proxy(auth("admin", "pass")).
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greetJSON").
        then().
                header("Via", not(emptyOrNullString()));
    }

    @Test public void
    using_proxy_without_ok_auth() {
        given().
                proxy(host("127.0.0.1").withPort(8888).withAuth("wrong", "pass")).
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greetJSON").
        then().
                statusCode(407);
    }

    // This tests makes sure that issue 693 is resolved
    @Test @Ignore  public void
    can_use_external_proxy() {
        given().
                proxy(host("pxy.int.ws.streamshield.net").withPort(3128).withAuth("user_384364539@wtqt.com", "Password1")).
        when().
                get("https://facebook.com").
        then().
                statusCode(200);
    }
}
