package name.martingeisse.majai.compiler;

import name.martingeisse.majai.compiler.runtime.GenericRuntimeObject;
import name.martingeisse.majai.vm.VmObjectMetadata;
import org.objectweb.asm.tree.FieldNode;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RuntimeObjectSerializer {

	private final Context context;
	private final PrintWriter out;

	public RuntimeObjectSerializer(Context context, PrintWriter out) {
		this.context = context;
		this.out = out;
	}

	protected abstract String getLabel(Object o);

	public void serialize(Object o) {
		if (o == null) {
			out.println("\t.word 0");
		} else if (o instanceof byte[]) {

			byte[] array = (byte[]) o;
			out.println("\t.word 0"); // TODO byte[] vtable
			serializeArray(array.length, ".byte", i -> Integer.toString(array[i] & 0xff));

		} else if (o instanceof short[]) {

			short[] array = (short[]) o;
			out.println("\t.word 0"); // TODO short[] vtable
			serializeArray(array.length, ".short", i -> Integer.toString(array[i] & 0xffff));

		} else if (o instanceof char[]) {

			char[] array = (char[]) o;
			out.println("\t.word 0"); // TODO char[] vtable
			serializeArray(array.length, ".short", i -> Integer.toString(array[i] & 0xffff));

		} else if (o instanceof Object[]) {

			Object[] array = (Object[]) o;
			out.println("\t.word 0"); // TODO Object[] vtable
			// TODO possibly element metadata, but have to add it to java.lang.ReferenceArray first
			serializeArray(array.length, ".word", i -> array[i] == null ? "0" : getLabel(array[i]));

		} else if (o instanceof String) {

			GenericRuntimeObject r = new GenericRuntimeObject("java/lang/String");
			r.put("characters", ((String)o).toCharArray());
			serializeGenericRuntimeObject(r);

		} else if (o instanceof GenericRuntimeObject) {

			serializeGenericRuntimeObject((GenericRuntimeObject)o);

		} else {
			throw new RuntimeException("cannot serialize for runtime: " + o);
		}
	}

	private void serializeArray(int length, String directive, Function<Integer, String> valueGetter) {
		out.println("\t.word " + length);
		out.print("\t" + directive + " ");
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				out.print(", ");
			}
			out.print(valueGetter.apply(i));
		}
		out.println();
		out.println("\t.align 2");
	}

	private void serializeGenericRuntimeObject(GenericRuntimeObject o) {

		ClassInfo classInfo = context.resolveClass(o.getClassName());
		VmObjectMetadata objectMetadata = (VmObjectMetadata)classInfo.runtimeMetadataContributor;
		o.put("vtable", objectMetadata.getVtable());

		List<FieldInfo> fields = getSortedFields(classInfo);
		int position = 0;
		for (FieldInfo field : fields) {
			int displacement = field.storageOffset - position;
			if (displacement < 0) {
				throw new RuntimeException();
			} else if (displacement > 0) {
				out.println("\t.fill " + displacement + ", 1, 0");
				position = field.storageOffset;
			}
			if (field.desc.startsWith("[") || field.desc.startsWith("L")) {
				out.println("\t.word " + getLabel(o.getFieldValues().get(field.name)));
			} else switch (field.desc) {

				default:
					throw new RuntimeException("cannot serialize field with descriptor " + field.desc);

			}
		}
	}

	private List<FieldInfo> getSortedFields(ClassInfo classInfo) {
		List<FieldInfo> result = new ArrayList<>();
		collectFields(classInfo, result);
		result.sort(Comparator.comparing(field -> field.storageOffset));
		return result;
	}

	private void collectFields(ClassInfo classInfo, List<FieldInfo> fields) {
		for (FieldNode field : classInfo.fields) {
			fields.add((FieldInfo)field);
		}
		if (classInfo.superName != null) {
			collectFields(context.resolveClass(classInfo.superName), fields);
		}
	}

	public interface Context {
		ClassInfo resolveClass(String name);
	}

}
