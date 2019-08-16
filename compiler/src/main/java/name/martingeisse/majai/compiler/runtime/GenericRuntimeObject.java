package name.martingeisse.majai.compiler.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class GenericRuntimeObject {

	private final String className;
	private final Map<String, Object> fieldValues = new HashMap<>();

	public GenericRuntimeObject(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	public Map<String, Object> getFieldValues() {
		return fieldValues;
	}

	public void put(String fieldName, Object fieldValue) {
		fieldValues.put(fieldName, fieldValue);
	}

}
