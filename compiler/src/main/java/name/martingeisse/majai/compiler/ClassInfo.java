package name.martingeisse.majai.compiler;

import name.martingeisse.majai.vm.VmObjectMetadataContributor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 *
 */
public class ClassInfo extends ClassNode {

	public FieldAllocator fieldAllocator;
	public VtableAllocator vtableAllocator;
	public VmObjectMetadataContributor runtimeMetadataContributor;

	public ClassInfo() {
		super(Opcodes.ASM7);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldInfo field = new FieldInfo(this, access, name, descriptor, signature, value);
		fields.add(field);
		return field;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodInfo method = new MethodInfo(this, access, name, descriptor, signature, exceptions);
		methods.add(method);
		return method;
	}

}
