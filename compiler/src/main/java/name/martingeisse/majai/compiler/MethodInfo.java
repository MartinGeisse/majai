package name.martingeisse.majai.compiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class MethodInfo extends MethodNode {

	public final ClassInfo containingClass;

	/**
	 * This index is using entry--sized (word-sized) units, but does not include the array header size. That is,
	 * the first method has index 0, the second has index 1, and so on.
	 */
	public int vtableIndex;

	public MethodInfo(ClassInfo containingClass, int access, String name, String descriptor, String signature, String[] exceptions) {
		super(Opcodes.ASM7, access, name, descriptor, signature, exceptions);
		this.containingClass = containingClass;
	}

}
