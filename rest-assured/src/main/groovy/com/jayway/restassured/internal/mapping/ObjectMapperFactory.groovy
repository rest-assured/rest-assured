package com.jayway.restassured.internal.mapping

import org.codehaus.jackson.map.ObjectMapper

class ObjectMapperFactory {
	private static List<IMapperConfig> map = new ArrayList<IMapperConfig>();

	public static void register(IMapperConfig config) {
		map.add(config);
	}

	public static ObjectMapper createJacksonObjectMapper() {
		def mapper = new ObjectMapper();

		for (IMapperConfig config:map) {
			config.configure(mapper);
		}

		return mapper;
	}
}
