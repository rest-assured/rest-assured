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

package io.restassured.examples.springmvc.support;

import io.restassured.RestAssured;
import io.restassured.examples.springmvc.config.WebApp;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ClassInheritanceHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.listener.IntrospectorCleaner;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.opentest4j.TestAbortedException;
import org.springframework.web.WebApplicationInitializer;

import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Disabled("To make Maven happy")
public class WithJetty {
    @BeforeAll
    public static void startJetty() throws Exception {
        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        wac.setClassLoader(Thread.currentThread().getContextClassLoader());
        wac.setParentLoaderPriority(true);
        wac.setConfigurations(new Configuration[]{
                new WebXmlConfiguration(),
                new AnnotationConfiguration() {
                    @Override
                    public void preConfigure(WebAppContext context) {
                        ConcurrentHashMap<String, Set<String>> map = new ClassInheritanceMap();
                        Set<String> set = new CopyOnWriteArraySet<>();
                        set.add(WebApp.class.getName());
                        map.put(WebApplicationInitializer.class.getName(), set);
                        context.setAttribute(CLASS_INHERITANCE_MAP, map);
                        _classInheritanceHandler = new ClassInheritanceHandler(map);
                    }
                }
        });
        wac.setServer(server);
        wac.addEventListener(new IntrospectorCleaner());

        server.setHandler(wac);
        try {
            server.start();
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                throw new TestAbortedException("Unable to start Jetty (socket not permitted in this environment)", ex);
            }
            if (ex instanceof MultiException) {
                MultiException me = (MultiException) ex;
                for (Throwable cause : me.getThrowables()) {
                    if (cause instanceof SocketException) {
                        throw new TestAbortedException("Unable to start Jetty (socket not permitted in this environment)", ex);
                    }
                }
            }
            throw ex;
        }
    }

    @BeforeEach
    public void setUpBeforeTest() {
        RestAssured.reset();
    }

    @AfterAll
    public static void stopJetty() throws Exception {
        server.stop();
        server.join();
    }

    private static Server server;
}
