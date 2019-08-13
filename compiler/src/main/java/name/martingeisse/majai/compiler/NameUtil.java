package name.martingeisse.majai.compiler;

import org.objectweb.asm.tree.MethodInsnNode;

/**
 *
 */
public final class NameUtil {

	// prevent instantiation
	private NameUtil() {
	}

	public static String normalizeClassName(String name) {
		return name.replace('.', '/');
	}

	public static String denormalizeClassName(String name) {
		return name.replace('/', '.');
	}

	public static String mangleClassName(String className) {
		return normalizeClassName(className).replace('/', '_');
	}

	public static String mangleClassName(ClassInfo classInfo) {
		return mangleClassName(classInfo.name);
	}

	public static String mangleMethodName(MethodInfo method) {
		return mangleClassName(method.containingClass) + '_' + method.name + '_' + method.desc.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_');
	}

	public static String mangleMethodName(MethodInsnNode call) {
		return mangleClassName(call.owner) + '_' + call.name + '_' + call.desc.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_');
	}

	public static String mangleFieldName(FieldInfo field) {
		return mangleClassName(field.containingClass) + '_' + field.name;
	}

}
