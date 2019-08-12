package name.martingeisse.majai.compiler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class RuntimeObjects {

	private final Map<Object, String> labels = new HashMap<>();

	public String getLabel(Object o) {
		return labels.computeIfAbsent(o, o2 -> "object" + labels.size());
	}

	public void emit(PrintWriter out) {
		out.println("//");
		out.println("// runtime objects");
		out.println("//");
		out.println("");
		out.println(".data");

		RuntimeObjectSerializer serializer = new RuntimeObjectSerializer(out) {
			@Override
			protected String getLabel(Object o) {
				return RuntimeObjects.this.getLabel(o);
			}
		};
		Set<Object> emittedObjects = new HashSet<>();
		while (emittedObjects.size() < labels.size()) {
			Map<Object, String> batch = new HashMap<>(labels);
			for (Map.Entry<Object, String> entry : batch.entrySet()) {
				if (!emittedObjects.add(entry.getKey())) {
					continue;
				}
				out.println(entry.getValue() + ":");
				serializer.serialize(entry.getKey());
			}
		}

		out.println();
	}

}