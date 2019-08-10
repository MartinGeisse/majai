package name.martingeisse.majai;

import org.objectweb.asm.FieldVisitor;
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

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldInfo field = new FieldInfo(access, name, descriptor, signature, value);
		fields.add(field);
		return field;
	}

	public void initializeClassInfo() {
	}

}
