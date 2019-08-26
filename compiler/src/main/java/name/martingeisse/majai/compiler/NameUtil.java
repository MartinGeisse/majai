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
        return mangleMethodName(method.containingClass.name, method.name, method.desc);
    }

    public static String mangleMethodName(MethodInsnNode call) {
        return mangleMethodName(call.owner, call.name, call.desc);
    }

    public static String mangleMethodName(String className, String methodName, String methodDescriptor) {
    	methodName = methodName.replace("<", "lt").replace(">", "gt");
		methodDescriptor = methodDescriptor.replace("(", "").replace(')', '_').replace(';', '_').replace('/', '_')
				.replace("[", "A_");
		return mangleClassName(className) + '_' + methodName + '_' + methodDescriptor;

    }

    public static String mangleFieldName(FieldInfo field) {
        return mangleClassName(field.containingClass) + '_' + field.name;
    }

}
