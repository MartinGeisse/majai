package name.martingeisse.majai;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

public class FieldInfo extends FieldNode {

    public int storageOffset;

    public FieldInfo(int access, String name, String descriptor, String signature, Object value) {
        super(Opcodes.ASM6, access, name, descriptor, signature, value);
    }

}
