package io.restassured.internal.path.json;

import groovy.json.JsonOutput;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

public class JsonPrettifier {
  public static String prettifyJson(String json) {
    String prettified = JsonOutput.prettyPrint(json);
    return new  UnicodeUnescaper().translate(prettified);
  }

}
