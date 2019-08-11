package name.martingeisse.majai.compiler;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

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

	public static String mangleMethodName(ClassInfo classInfo, MethodNode methodNode) {
		return mangleClassName(classInfo) + '_' + methodNode.name + '_' + methodNode.desc.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_');
	}

	public static String mangleMethodName(MethodInsnNode call) {
		return mangleClassName(call.owner) + '_' + call.name + '_' + call.desc.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_');
	}

	public static String mangleFieldName(ClassInfo classInfo, FieldNode fieldNode) {
		return mangleClassName(classInfo) + '_' + fieldNode.name;
	}

}
