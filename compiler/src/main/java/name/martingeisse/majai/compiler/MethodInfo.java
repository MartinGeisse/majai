package name.martingeisse.majai.compiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class MethodInfo extends MethodNode {

	/**
	 * This index is using entry--sized (word-sized) units, but does not include the array header size. That is,
	 * the first method has index 0, the second has index 1, and so on.
	 */
	public int vtableIndex;

	public MethodInfo(int access, String name, String descriptor, String signature, String[] exceptions) {
		super(Opcodes.ASM6, access, name, descriptor, signature, exceptions);
	}

}
