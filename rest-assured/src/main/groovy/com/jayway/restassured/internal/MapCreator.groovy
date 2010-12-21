package com.jayway.restassured.internal

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/21/10
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
class MapCreator {

   def static Map<String, Object> createMapFromStrings(String firstParam, ... parameters) {
      return createMapFromObjects(createArgumentArray(firstParam, parameters));
   }

   def static Map<String, Object> createMapFromObjects(... parameters) {
    if(parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < parameters.length; i+=2) {
      map.put(parameters[i], parameters[i+1]);
    }
    return map;
  }

  private static Object[] createArgumentArray(String firstArgument, Object... additionalArguments) {
    def params = [firstArgument]
    additionalArguments.each {
      params << it
    }
    return params as Object[]
  }

}
