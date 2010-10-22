package com.jayway.restassured.itest

import org.junit.AfterClass
import org.junit.BeforeClass
import org.mortbay.jetty.Server
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.webapp.WebAppContext

class WithJetty {
  @BeforeClass
  static void startJetty() throws Exception {
    server = new Server();
    org.mortbay.jetty.Connector connector = new SelectChannelConnector();
    connector.setPort(8080);
    server.addConnector(connector);

    WebAppContext wac = new WebAppContext();
    wac.setContextPath("/");
    def canonicalPath = new File(".").getCanonicalPath()
    def scalatraPath = "/examples/scalatra-webapp";
    def itestPath = "/examples/rest-assured-itest";
    def webAppPath = "/src/main/webapp";
    def path = canonicalPath.contains(itestPath) ? new File("../../.").getCanonicalPath()+scalatraPath+webAppPath : canonicalPath+scalatraPath+webAppPath;
    wac.setWar(path);
    wac.setServer(server)

    server.setHandler(wac)
    server.setStopAtShutdown(true);
    server.start();
  }

  @AfterClass
  static void stopJetty() throws Exception {
    server.stop();
  }

  private static Server server;
}