package name.martingeisse.majai;

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

	public static String mangleClassName(ClassInfo classInfo) {
		return normalizeClassName(classInfo.name).replace('/', '_');
	}

	public static String mangleMethodName(ClassInfo classInfo, MethodNode methodNode) {
		return mangleClassName(classInfo) + '_' + methodNode.name + '_' + methodNode.desc.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_');
	}

}
