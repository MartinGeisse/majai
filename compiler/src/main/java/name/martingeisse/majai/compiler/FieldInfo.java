package name.martingeisse.majai.compiler;

import name.martingeisse.majai.compiler.descriptor.ParsedFieldDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

public class FieldInfo extends FieldNode {

    public final ClassInfo containingClass;
    public final ParsedFieldDescriptor parsedDescriptor;
    public int storageOffset;

    public FieldInfo(ClassInfo containingClass, int access, String name, String descriptor, String signature, Object value) {
        super(Opcodes.ASM7, access, name, descriptor, signature, value);
        this.containingClass = containingClass;
        this.parsedDescriptor = new ParsedFieldDescriptor(descriptor);
    }

}
