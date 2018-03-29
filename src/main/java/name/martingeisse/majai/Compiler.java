package name.martingeisse.majai;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Compiler {

	private final ImmutableMap<String, ClassNode> classNodes;

	public Compiler(Collection<ClassNode> classNodes) {
		Map<String, ClassNode> map = new HashMap<>();
		for (ClassNode classNode : classNodes) {
			map.put(classNode.name, classNode);
		}
		this.classNodes = ImmutableMap.copyOf(map);
	}

	public ImmutableMap<String, ClassNode> getClassNodes() {
		return classNodes;
	}

	public byte[] compile() {
		for (ClassNode classNode : classNodes.values()) {
			for (MethodNode methodNode : classNode.methods) {
				if (methodNode.name.equals("<init>")) {
					continue;
				}
				if (methodNode.name.equals("<clinit>")) {
					throw new UnsupportedOperationException("static initializer not yet supported");
				}
				if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
					throw new UnsupportedOperationException("only static methods allowed: " + methodNode.name);
				}

			}
		}
		return new byte[] {
			VirtualMachine.OPCODE_INT32,
			0,
			0,
			0,
			2,
			VirtualMachine.OPCODE_INT32,
			0,
			0,
			0,
			2,
			VirtualMachine.OPCODE_ADD,
			VirtualMachine.OPCODE_DUP,
			VirtualMachine.OPCODE_WRITE,
			VirtualMachine.OPCODE_INT32,
			0,
			0,
			0,
			1,
			VirtualMachine.OPCODE_SUB,
			VirtualMachine.OPCODE_WRITE,
			VirtualMachine.OPCODE_EXIT,
		};
	}

}
