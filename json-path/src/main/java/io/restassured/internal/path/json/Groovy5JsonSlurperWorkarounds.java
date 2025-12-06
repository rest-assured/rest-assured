package io.restassured.internal.path.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workarounds for Groovy 5 {@code JsonSlurper} / GPath behaviour when JSON fields are named {@code "properties"}.
 * <p>
 * Background: starting with Groovy 5, the objects returned by {@code JsonSlurper} expose the Groovy
 * meta-property {@code properties} (and other meta-properties) in a way that takes precedence over
 * JSON keys named {@code "properties"}. This breaks existing JsonPath / GPath expressions that rely on:
 * <ul>
 *   <li>{@code root.properties.height}</li>
 *   <li>{@code [0].properties}</li>
 *   <li>{@code features.properties.gridId}</li>
 * </ul>
 * which in Groovy 4 navigated into the JSON field {@code "properties"}, but in Groovy 5 resolve to
 * meta-data (e.g. {@code [class: ..., empty: ...]}) or {@code null}.
 * <p>
 * This helper centralizes the runtime workarounds needed to restore Groovy 4–style behaviour on Groovy 5+:
 * <ul>
 *   <li>Provides explicit {@link ProxyMap#getProperties()} method for maps, blocking Groovy's access to
 *   the meta-property, forcing {@code map.properties} (and any other property name) to use only map entries.</li>
 *   <li>Provides explicit {@link ProxyArray#getProperties()} method for lists so that for lists of JSON objects
 *   that have a {@code "properties"} key, {@code list.properties} behaves like Groovy 4 GPath and returns
 *   {@code list.collect { it['properties'] }} instead of the list’s meta-properties.</li>
 * </ul>
 *  This allows existing JsonPath expressions that use {@code "properties"} as a JSON field name to continue
 *  working transparently across Groovy versions.
 */
class Groovy5JsonSlurperWorkarounds {

  private static final String PROPERTIES = "properties";

  static Map<String, Object> newMap() {
    return new ProxyMap();
  }

  static class ProxyMap extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public Object getProperties() {
      return super.get(PROPERTIES);
    }
  }

  static List<Object> newList() {
    return new ProxyArray();
  }

  static class ProxyArray extends ArrayList<Object> {
    private static final long serialVersionUID = 1L;

    public Object getProperties() {
      if (!isEmpty() && get(0) instanceof Map<?, ?> entry0 && entry0.containsKey(PROPERTIES)) {
        // Groovy 4–style JSON behaviour:
        // [map, map].properties -> [map['properties'], ...]
        return stream().map(elem -> {
          if (elem instanceof Map<?, ?> map && map.containsKey(PROPERTIES)) {
            return map.get(PROPERTIES);
          } else {
            return null;
          }
        }).toList();
      }
      return null;
    }
  }

  private Groovy5JsonSlurperWorkarounds() {
    // No instances
  }
}
