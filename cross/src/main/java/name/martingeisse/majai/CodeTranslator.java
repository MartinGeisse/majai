package name.martingeisse.majai;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.PrintWriter;

/**
 *
 */
class CodeTranslator {

	private final PrintWriter out;
	private final ClassInfo classInfo;
	private final MethodNode methodNode;
	private final String mangledMethodName;

	CodeTranslator(PrintWriter out, ClassInfo classInfo, MethodNode methodNode) {
		this.out = out;
		this.classInfo = classInfo;
		this.methodNode = methodNode;
		this.mangledMethodName = NameUtil.mangleMethodName(classInfo, methodNode);
	}

	void translate() {
		if (methodNode.name.equals("<init>")) {
			return;
		}
		if (methodNode.name.equals("<clinit>")) {
			throw new UnsupportedOperationException("static initializer not yet supported");
		}
		if ((methodNode.access & Opcodes.ACC_NATIVE) != 0) {
			return;
		}
		if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
			throw new UnsupportedOperationException("only static methods allowed: " + methodNode.name);
		}

		// intro
		out.println(mangledMethodName + ':');
		out.println("	sub sp, sp, " + (methodNode.maxLocals + 2) * 4);
		out.println("	sw ra, " + ((methodNode.maxLocals) * 4) + "(sp)");
		out.println("	sw s0, " + ((methodNode.maxLocals + 1) * 4) + "(sp)");

		// code
		for (AbstractInsnNode instruction = methodNode.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
			translate(instruction);
		}

		// outro
		out.println(mangledMethodName + "__return:");
		out.println("	lw s0, " + ((methodNode.maxLocals + 1) * 4) + "(sp)");
		out.println("	lw ra, " + ((methodNode.maxLocals) * 4) + "(sp)");
		out.println("	add sp, sp, " + (methodNode.maxLocals + 2) * 4);
		out.println("	ret");
		out.println("");

	}

	private void translate(AbstractInsnNode instruction) {
		if (instruction instanceof LdcInsnNode) {
			LdcInsnNode ldc = (LdcInsnNode) instruction;
			if (ldc.cst instanceof Integer) {
				out.println("	li s1, " + ldc.cst);
				push("s1");
			} else {
				throw new RuntimeException("ldc with anything other than Integer not supported yet; found: " + ldc.cst.getClass());
			}
		}
		out.println("	// " + instruction);
	}

	private void push(String register) {
		out.println("	sub sp, sp, 4");
		out.println("	sw " + register + ", 0(sp)");
	}

	private void pop(String register) {
		out.println("	lw " + register + ", 0(sp)");
		out.println("	add sp, sp, 4");
	}

}
