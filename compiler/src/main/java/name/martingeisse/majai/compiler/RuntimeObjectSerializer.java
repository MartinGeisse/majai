package name.martingeisse.majai.compiler;

import java.io.PrintWriter;

public abstract class RuntimeObjectSerializer {

	private final PrintWriter out;

	public RuntimeObjectSerializer(PrintWriter out) {
		this.out = out;
	}

	protected abstract String getLabel(Object o);

	public void serialize(Object o) {
		if (o == null) {
			out.println("\t.word 0");
		} else if (o instanceof byte[]) {
			byte[] array = (byte[]) o;
			out.println("\t.word 0"); // TODO byte[] vtable
			out.println("\t.word " + array.length);
			out.print("\t.byte ");
			boolean first = true;
			for (byte b : array) {
				if (first) {
					first = false;
				} else {
					out.print(", ");
				}
				out.print(b & 0xff);
			}
			out.println();
		} else if (o instanceof Object[]) {
			Object[] array = (Object[]) o;
			out.println("\t.word 0"); // TODO Object[] vtable
			// TODO possibly element metadata, but have to add it to java.lang.ReferenceArray first
			out.println("\t.word " + array.length);
			out.print("\t.word ");
			boolean first = true;
			for (Object element : array) {
				if (first) {
					first = false;
				} else {
					out.print(", ");
				}
				out.print(getLabel(element));
			}
			out.println();
		} else {
			throw new RuntimeException("cannot serialize for runtime: " + o);
		}
	}

}
