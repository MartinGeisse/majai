package name.martingeisse.majai.compiler;

import name.martingeisse.majai.compiler.runtime.LabelReference;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class RuntimeObjects {

	private final Context context;
	private final Map<Object, String> labels = new HashMap<>();

	public RuntimeObjects(Context context) {
		this.context = context;
	}

	public String getLabel(Object o) {
		if (o instanceof LabelReference) {
			return ((LabelReference) o).getLabel();
		} else {
			return labels.computeIfAbsent(o, o2 -> "object" + labels.size());
		}
	}

	public void emit(PrintWriter out) {
		out.println("//");
		out.println("// runtime objects");
		out.println("//");
		out.println("");
		out.println(".data");

		RuntimeObjectSerializer serializer = new RuntimeObjectSerializer(context::resolveClass, out) {
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
				if (!(entry.getKey() instanceof LabelReference)) {
					serializer.serialize(entry.getKey());
				}
			}
		}

		out.println();
	}

	public interface Context {
		ClassInfo resolveClass(String name);
	}

}
