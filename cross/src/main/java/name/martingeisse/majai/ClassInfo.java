package name.martingeisse.majai;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 *
 */
public class ClassInfo extends ClassNode {

	public FieldAllocator fieldAllocator;

	public ClassInfo() {
		super(Opcodes.ASM6);
	}

	public void initializeClassInfo() {
	}

}
