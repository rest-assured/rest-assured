package io.restassured.module.spring.commons;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class ParamLogger {

	private Map<String, Object> map;

	protected ParamLogger(Map<String, Object> parameters) {
		this.map = parameters;
	}

	public void logParams() {
		for (Map.Entry<String, Object> stringListEntry : map.entrySet()) {
			Object value = stringListEntry.getValue();
			Collection<Object> values;
			if (value instanceof Collection) {
				values = (Collection<Object>) value;
			} else {
				values = new ArrayList<Object>();
				values.add(value);
			}

			for (Object theValue : values) {
				logParam(stringListEntry.getKey(), theValue);
			}
		}
	}

	protected abstract void logParam(String paramName, Object paramValue);
}