package io.restassured.module.spring.commons;

import java.util.Collection;
import java.util.Map;

/**
 * @author Olga Maciaszek-Sharma
 */
public abstract class ParamApplier {

	private Map<String, Object> map;

	protected ParamApplier(Map<String, Object> parameters) {
		this.map = parameters;
	}

	public void applyParams() {
		for (Map.Entry<String, Object> listEntry : map.entrySet()) {
			Object value = listEntry.getValue();
			String[] stringValues;
			if (value instanceof Collection) {
				Collection col = (Collection) value;
				stringValues = new String[col.size()];
				int index = 0;
				for (Object val : col) {
					stringValues[index] = val == null ? null : val.toString();
					index++;
				}
			} else {
				stringValues = new String[1];
				stringValues[0] = value == null ? null : value.toString();
			}
			applyParam(listEntry.getKey(), stringValues);
		}
	}

	protected abstract void applyParam(String paramName, String[] paramValues);
}
