package name.martingeisse.majai.compiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

public class FieldInfo extends FieldNode {

    public final ClassInfo containingClass;
    public int storageOffset;

    public FieldInfo(ClassInfo containingClass, int access, String name, String descriptor, String signature, Object value) {
        super(Opcodes.ASM6, access, name, descriptor, signature, value);
        this.containingClass = containingClass;
    }

}
