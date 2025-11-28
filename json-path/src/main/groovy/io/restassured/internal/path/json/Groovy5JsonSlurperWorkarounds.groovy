package io.restassured.internal.path.json


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
 *   <li>Patches the meta-classes for {@code LazyMap} and {@code LinkedHashMap} so that
 * {@code map.properties} (and any other property name) first checks {@code map.containsKey(name)}
 *       and returns the JSON value when present, only falling back to the original meta-property
 *       resolution if the JSON key does not exist.</li>
 *   <li>Patches {@code ArrayList} so that for lists of JSON objects that have a {@code "properties"} key,
 * {@code list.properties} behaves like Groovy 4 GPath and returns
 * {@code list.collect { it['properties'] }} instead of the list’s meta-properties.</li>
 * </ul>
 * The patches are applied only when running on Groovy 5 or newer (as detected via {@code GroovySystem.version}),
 * and leave Groovy 4 behaviour unchanged. This allows existing JsonPath expressions that use
 * {@code "properties"} as a JSON field name to continue working transparently across Groovy versions.
 */
class Groovy5JsonSlurperWorkarounds {

  static void initialize() {
    if (isGroovy5Plus()) {
      patchJsonMapPropertyResolution()
      patchListPropertiesResolution()
    }
  }

  static boolean isGroovy5Plus() {
    try {
      def v = GroovySystem.version // e.g. "4.0.23", "5.0.0"
      def major = v.tokenize('.')[0] as int
      return major >= 5
    } catch (ignored) {
      return false
    }
  }

  /**
   * For JSON objects (LazyMap), prefer the JSON field over meta-properties.
   */
  private static void patchJsonMapPropertyResolution() {
    // Patch both LazyMap and LinkedHashMap: whichever Groovy uses for JSON objects
    patchMapClass('org.apache.groovy.json.internal.LazyMap')
    patchMapClass('groovy.json.internal.LazyMap')          // for older Groovy, just in case
    patchMapClass('java.util.LinkedHashMap')
  }

  private static void patchMapClass(String className) {
    Class mapClass
    try {
      mapClass = Class.forName(className)
    } catch (ClassNotFoundException ignore) {
      return
    }

    def registry = GroovySystem.metaClassRegistry
    def originalMetaClass = registry.getMetaClass(mapClass)

    mapClass.metaClass.getProperty = { String name ->
      Map self = (Map) delegate

      // Prefer JSON key if it exists
      if (self.containsKey(name)) {
        return self.get(name)
      }

      // Fallback to original behaviour (meta-properties, etc.)
      return originalMetaClass.getProperty(delegate, name)
    }
  }

  /**
   * For lists of JSON objects that have a "properties" key, make
   * list.properties behave like Groovy 4 GPath: list.collect { it.properties }.
   */
  private static void patchListPropertiesResolution() {
    Class listClass = ArrayList

    def registry = GroovySystem.metaClassRegistry
    def originalMetaClass = registry.getMetaClass(listClass)

    listClass.metaClass.getProperty = { String name ->
      List self = (List) delegate

      if (name == 'properties'
              && !self.isEmpty()
              && self[0] instanceof Map
              && ((Map) self[0]).containsKey('properties')) {

        // Groovy 4–style JSON behaviour: [map, map].properties -> [map['properties'], ...]
        return self.collect { elem ->
          if (elem instanceof Map && ((Map) elem).containsKey('properties')) {
            ((Map) elem).get('properties')
          } else {
            null
          }
        }
      }

      // For all other properties, keep original Groovy semantics,
      // including GPath-style spreading for gridId, type, etc.
      return originalMetaClass.getProperty(delegate, name)
    }
  }
}