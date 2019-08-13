package name.martingeisse.majai.compiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.PrintWriter;

/**
 *
 */
class CodeTranslator {

	private final Context context;
	private final PrintWriter out;
	private final MethodInfo methodInfo;
	private final String mangledMethodName;
	private final String returnLabel;

	CodeTranslator(Context context, PrintWriter out, MethodInfo methodInfo) {
		this.context = context;
		this.out = out;
		this.methodInfo = methodInfo;
		this.mangledMethodName = NameUtil.mangleMethodName(methodInfo);
		this.returnLabel = mangledMethodName + "__return";
	}

	void translate() {
		if (methodInfo.name.equals("<init>")) {
			return;
		}
		if (methodInfo.name.equals("<clinit>")) {
			throw new UnsupportedOperationException("static initializer not yet supported");
		}
		if ((methodInfo.access & Opcodes.ACC_NATIVE) != 0) {
			return;
		}
		if ((methodInfo.access & Opcodes.ACC_STATIC) == 0) {
			throw new UnsupportedOperationException("only static methods allowed: " + methodInfo.name);
		}

		// intro
		out.println(mangledMethodName + ':');
		out.println("	addi sp, sp, -" + (methodInfo.maxLocals + 2) * 4);
		out.println("	sw ra, " + ((methodInfo.maxLocals) * 4) + "(sp)");
		out.println("	sw s0, " + ((methodInfo.maxLocals + 1) * 4) + "(sp)");
		out.println("	mv s0, sp");
		{
			int words = (methodInfo.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
			words += new ParsedMethodDescriptor(methodInfo.desc).getParameterWords();
			for (int i = 0; i < words; i++) {
				out.println("	sw a" + i + ", " + (i * 4) + "(s0)");
			}
		}

		// code
		for (AbstractInsnNode instruction = methodInfo.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
			translate(instruction);
		}

		// outro
		out.println(returnLabel + ':');
		out.println("	lw s0, " + ((methodInfo.maxLocals + 1) * 4) + "(sp)");
		out.println("	lw ra, " + ((methodInfo.maxLocals) * 4) + "(sp)");
		out.println("	add sp, sp, " + (methodInfo.maxLocals + 2) * 4);
		out.println("	ret");
		out.println("");

	}

	private void translate(AbstractInsnNode instruction) {
		int opcode = instruction.getOpcode();
		switch (opcode) {

			case Opcodes.NOP:
				break;

			case Opcodes.ACONST_NULL:
				push("x0");
				break;

			case Opcodes.ICONST_M1:
			case Opcodes.ICONST_0:
			case Opcodes.ICONST_1:
			case Opcodes.ICONST_2:
			case Opcodes.ICONST_3:
			case Opcodes.ICONST_4:
			case Opcodes.ICONST_5:
				pushInt(opcode - Opcodes.ICONST_0);
				break;

			case Opcodes.LCONST_0:
			case Opcodes.LCONST_1:
				pushLong(opcode - Opcodes.LCONST_0);
				break;

			case Opcodes.FCONST_0:
			case Opcodes.FCONST_1:
			case Opcodes.FCONST_2:
			case Opcodes.DCONST_0:
			case Opcodes.DCONST_1:
				throw new NotYetImplementedException();

			case Opcodes.BIPUSH:
			case Opcodes.SIPUSH:
				//noinspection ConstantConditions
				pushInt(((IntInsnNode) instruction).operand);
				break;

			case Opcodes.LDC: {
				LdcInsnNode ldc = (LdcInsnNode) instruction;
				if (ldc.cst instanceof Integer) {
					pushInt((Integer) ldc.cst);
				} else {
					throw new NotYetImplementedException("ldc with anything other than Integer not supported yet; found: " + ldc.cst.getClass());
				}
				break;
			}

			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.ALOAD:
				//noinspection ConstantConditions
				load32(((VarInsnNode)instruction).var);
				break;

			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
				//noinspection ConstantConditions
				load64(((VarInsnNode)instruction).var);
				break;

			case Opcodes.IALOAD:
			case Opcodes.FALOAD:
			case Opcodes.AALOAD:
				writeArrayLoad(2, false, "lw");
				break;

			case Opcodes.BALOAD:
				writeArrayLoad(0, false, "lb");
				break;

			case Opcodes.CALOAD:
				writeArrayLoad(1, false, "lhu");
				break;

			case Opcodes.SALOAD:
				writeArrayLoad(1, false, "lh");
				break;

			case Opcodes.LALOAD:
			case Opcodes.DALOAD:
				writeArrayLoad(3, true, null);
				break;

			case Opcodes.ISTORE:
			case Opcodes.FSTORE:
			case Opcodes.ASTORE:
				//noinspection ConstantConditions
				store32(((VarInsnNode)instruction).var);
				break;

			case Opcodes.LSTORE:
			case Opcodes.DSTORE:
				//noinspection ConstantConditions
				store32(((VarInsnNode)instruction).var);
				break;

			case Opcodes.IASTORE:
			case Opcodes.FASTORE:
			case Opcodes.AASTORE:
				writeArrayStore(2, false, "sw");
				break;

			case Opcodes.LASTORE:
			case Opcodes.DASTORE:
				writeArrayStore(3, true, null);
				break;

			case Opcodes.BASTORE:
				writeArrayStore(0, false, "sb");
				break;

			case Opcodes.CASTORE:
			case Opcodes.SASTORE:
				writeArrayStore(1, false, "sh");
				break;

			case Opcodes.POP:
				out.println("	add sp, sp, 4");
				break;

			case Opcodes.POP2:
				out.println("	add sp, sp, 8");
				break;

			case Opcodes.DUP:
				out.println("	lw t0, 0(sp)");
				out.println("	addi sp, sp, -4");
				out.println("	sw t0, 0(sp)");
				break;

			case Opcodes.DUP_X1:
				out.println("	lw t0, 0(sp)");
				out.println("	lw t1, 4(sp)");
				out.println("	addi sp, sp, -4");
				out.println("	sw t0, 0(sp)");
				out.println("	sw t1, 4(sp)");
				out.println("	sw t0, 8(sp)");
				break;

			case Opcodes.DUP_X2:
				out.println("	lw t0, 0(sp)");
				out.println("	lw t1, 4(sp)");
				out.println("	lw t2, 8(sp)");
				out.println("	addi sp, sp, -4");
				out.println("	sw t0, 0(sp)");
				out.println("	sw t1, 4(sp)");
				out.println("	sw t2, 8(sp)");
				out.println("	sw t0, 0(sp)");
				break;

			case Opcodes.DUP2_X1:
				out.println("	lw t0, 0(sp)");
				out.println("	lw t1, 4(sp)");
				out.println("	lw t2, 8(sp)");
				out.println("	addi sp, sp, -4");
				out.println("	sw t0, 0(sp)");
				out.println("	sw t1, 4(sp)");
				out.println("	sw t2, 8(sp)");
				out.println("	sw t0, 12(sp)");
				out.println("	sw t1, 16(sp)");
				break;

			case Opcodes.DUP2_X2:
				out.println("	lw t0, 0(sp)");
				out.println("	lw t1, 4(sp)");
				out.println("	lw t2, 8(sp)");
				out.println("	lw t3, 8(sp)");
				out.println("	addi sp, sp, -8");
				out.println("	sw t0, 0(sp)");
				out.println("	sw t1, 4(sp)");
				out.println("	sw t2, 8(sp)");
				out.println("	sw t3, 12(sp)");
				out.println("	sw t0, 16(sp)");
				out.println("	sw t1, 20(sp)");
				break;

			case Opcodes.SWAP:
				out.println("	lw t0, 0(sp)");
				out.println("	lw t1, 4(sp)");
				out.println("	sw t1, 0(sp)");
				out.println("	sw t0, 4(sp)");
				break;

			case Opcodes.IADD:
				wordOp("add");
				break;

			case Opcodes.LADD:
			case Opcodes.FADD:
			case Opcodes.DADD:
				throw new NotYetImplementedException();

			case Opcodes.ISUB:
				wordOp("sub");
				break;

			case Opcodes.LSUB:
			case Opcodes.FSUB:
			case Opcodes.DSUB:
				throw new NotYetImplementedException();

			case Opcodes.IMUL:
				wordOp("mul");
				break;

			case Opcodes.LMUL:
			case Opcodes.FMUL:
			case Opcodes.DMUL:
				throw new NotYetImplementedException();

			case Opcodes.IDIV:
				wordOp("div");
				break;

			case Opcodes.LDIV:
			case Opcodes.FDIV:
			case Opcodes.DDIV:
				throw new NotYetImplementedException();

			case Opcodes.IREM:
				wordOp("rem");
				break;

			case Opcodes.LREM:
			case Opcodes.FREM:
			case Opcodes.DREM:
				throw new NotYetImplementedException();

			case Opcodes.INEG:
				wordOp("neg");
				break;

			case Opcodes.LNEG:
			case Opcodes.FNEG:
			case Opcodes.DNEG:
				throw new NotYetImplementedException();

			case Opcodes.ISHL:
				wordOp("sll");
				break;

			case Opcodes.LSHL:
				throw new NotYetImplementedException();

			case Opcodes.ISHR:
				wordOp("sra");
				break;

			case Opcodes.LSHR:
				throw new NotYetImplementedException();

			case Opcodes.IUSHR:
				wordOp("srl");
				break;

			case Opcodes.LUSHR:
				throw new NotYetImplementedException();

			case Opcodes.IAND:
				wordOp("and");
				break;

			case Opcodes.LAND:
				throw new NotYetImplementedException();

			case Opcodes.IOR:
				wordOp("or");
				break;

			case Opcodes.LOR:
				throw new NotYetImplementedException();

			case Opcodes.IXOR:
				wordOp("xor");
				break;

			case Opcodes.LXOR:
				throw new NotYetImplementedException();

			case Opcodes.IINC: {
				IincInsnNode inc = (IincInsnNode)instruction;
				out.println("	lw t0, " + (inc.var * 4) + "(s0)");
				out.println("	addi t0, t0, " + inc.incr);
				out.println("	sw t0, " + (inc.var * 4) + "(s0)");
				break;
			}

			case Opcodes.I2L:
			case Opcodes.I2F:
			case Opcodes.I2D:
			case Opcodes.L2I:
			case Opcodes.L2F:
			case Opcodes.L2D:
			case Opcodes.F2I:
			case Opcodes.F2L:
			case Opcodes.F2D:
			case Opcodes.D2I:
			case Opcodes.D2L:
			case Opcodes.D2F:
				throw new NotYetImplementedException();

			case Opcodes.I2B:
				out.println("	lb t0, 0(sp)");
				out.println("	sw t0, 0(sp)");
				break;

			case Opcodes.I2C:
				out.println("	lhu t0, 0(sp)");
				out.println("	sw t0, 0(sp)");
				break;

			case Opcodes.I2S:
				out.println("	lh t0, 0(sp)");
				out.println("	sw t0, 0(sp)");
				break;

			case Opcodes.LCMP:
			case Opcodes.FCMPL:
			case Opcodes.FCMPG:
			case Opcodes.DCMPL:
			case Opcodes.DCMPG:
				throw new NotYetImplementedException();

			case Opcodes.IFEQ:
				branch(instruction, "beq", true);
				break;

			case Opcodes.IFNE:
				branch(instruction, "bne", true);
				break;

			case Opcodes.IFLT:
				branch(instruction, "blt", true);
				break;

			case Opcodes.IFGE:
				branch(instruction, "bge", true);
				break;

			case Opcodes.IFGT:
				branch(instruction, "bgt", true);
				break;

			case Opcodes.IFLE:
				branch(instruction, "ble", true);
				break;

			case Opcodes.IF_ICMPEQ:
				branch(instruction, "beq", false);
				break;

			case Opcodes.IF_ICMPNE:
				branch(instruction, "bne", false);
				break;

			case Opcodes.IF_ICMPLT:
				branch(instruction, "blt", false);
				break;

			case Opcodes.IF_ICMPGE:
				branch(instruction, "bge", false);
				break;

			case Opcodes.IF_ICMPGT:
				branch(instruction, "bgt", false);
				break;

			case Opcodes.IF_ICMPLE:
				branch(instruction, "ble", false);
				break;

			case Opcodes.IF_ACMPEQ:
				branch(instruction, "beq", false);
				break;

			case Opcodes.IF_ACMPNE:
				branch(instruction, "bne", false);
				break;

			case Opcodes.GOTO:
				out.println("	j " + getTargetLabel(instruction));
				break;

			case Opcodes.JSR:
			case Opcodes.RET:
			case Opcodes.TABLESWITCH:
			case Opcodes.LOOKUPSWITCH:
				throw new NotYetImplementedException();

			case Opcodes.IRETURN:
			case Opcodes.FRETURN:
			case Opcodes.ARETURN:
				pop("a0");
				out.println("	j " + returnLabel);
				break;

			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
				pop("a0");
				pop("a1");
				out.println("	j " + returnLabel);
				break;

			case Opcodes.RETURN:
				out.println("	j " + returnLabel);
				break;

			case Opcodes.GETSTATIC: {
				writeGetstatic(resolveField((FieldInsnNode)instruction));
				break;
			}

			case Opcodes.PUTSTATIC: {
				writePutstatic(resolveField((FieldInsnNode)instruction));
				break;
			}

			case Opcodes.GETFIELD: {
				writeGetfield(resolveField((FieldInsnNode)instruction));
				break;
			}

			case Opcodes.PUTFIELD: {
				writePutfield(resolveField((FieldInsnNode)instruction));
				break;
			}

			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
				throw new NotYetImplementedException();

			case Opcodes.INVOKESTATIC: {
				MethodInsnNode call = (MethodInsnNode)instruction;
				ParsedMethodDescriptor parsedMethodDescriptor = new ParsedMethodDescriptor(call.desc);
				for (int i = 0; i < parsedMethodDescriptor.getParameterWords(); i++) {
					out.println("	lw a" + i + ", " + (4 * (parsedMethodDescriptor.getParameterWords() - 1 - i)) + "(sp)");
				}
				out.println("	addi sp, sp, " + (4 * parsedMethodDescriptor.getParameterWords()));
				out.println("	call " + NameUtil.mangleMethodName(call));
				if (parsedMethodDescriptor.getReturnWords() == 1) {
					out.println("	addi sp, sp, -4");
					out.println("	sw a0, 0(sp)");
				} else if (parsedMethodDescriptor.getReturnWords() == 2) {
					out.println("	addi sp, sp, -8");
					out.println("	sw a0, 0(sp)");
					out.println("	sw a1, 4(sp)");
				}
				break;
			}

			case Opcodes.INVOKEINTERFACE:
			case Opcodes.INVOKEDYNAMIC:
				throw new NotYetImplementedException();

			case Opcodes.NEW: {
				TypeInsnNode typeInstruction = (TypeInsnNode)instruction;
				String className = typeInstruction.desc;
				context.resolveClass(className);
				out.println("	call allocateMemory");
				push("a0");
			}

			case Opcodes.NEWARRAY:
			case Opcodes.ANEWARRAY:
				throw new NotYetImplementedException();

			case Opcodes.ARRAYLENGTH:
				writeGetfield(resolveField(context.resolveClass("java.lang.Array"), "length", true));
				break;

			case Opcodes.ATHROW:
			case Opcodes.CHECKCAST:
			case Opcodes.INSTANCEOF:
			case Opcodes.MONITORENTER:
			case Opcodes.MONITOREXIT:
			case Opcodes.MULTIANEWARRAY:
				throw new NotYetImplementedException();

			case Opcodes.IFNULL:
				branch(instruction, "beq", true);
				break;

			case Opcodes.IFNONNULL:
				branch(instruction, "bne", true);
				break;

			default:
				throw new RuntimeException("unknown opcode: " + opcode);

		}
	}

	private void pushInt(int value) {
		out.println("	li t0, " + value);
		push("t0");
	}

	private void pushLong(long value) {
		pushInt((int) (value >> 32));
		pushInt((int) value);
	}

	private void load32(int index) {
		out.println("	lw t0, " + (index * 4) + "(s0)");
		push("t0");
	}

	private void store32(int index) {
		pop("t0");
		out.println("	sw t0, " + (index * 4) + "(s0)");
	}

	private void load64(int index) {
		load32(index + 1);
		load32(index);
	}

	private void store64(int index) {
		store32(index);
		store32(index + 1);
	}

	private void push(String register) {
		out.println("	addi sp, sp, -4");
		out.println("	sw " + register + ", 0(sp)");
	}

	private void pop(String register) {
		out.println("	lw " + register + ", 0(sp)");
		out.println("	add sp, sp, 4");
	}

	private void peek(String register) {
		out.println("	lw " + register + ", 0(sp)");
	}

	private void wordOp(String instruction) {
		out.println("	lw t0, 4(sp)");
		out.println("	lw t1, 0(sp)");
		out.println("	" + instruction + " t0, t0, t1");
		out.println("	add sp, sp, 4");
		out.println("	sw t0, 0(sp)");
	}

	private void branch(AbstractInsnNode bytecodeInstruction, String machineInstruction, boolean implicitZero) {
		if (!implicitZero) {
			pop("t1");
		}
		pop("t0");
		out.println("	" + machineInstruction + " t0, " + (implicitZero ? "x0" : "t1") + ", " + getTargetLabel(bytecodeInstruction));
	}

	private String getTargetLabel(AbstractInsnNode instruction) {
		return mangledMethodName + '_' + ((JumpInsnNode)instruction).label.getLabel().getOffset();
	}

	private FieldNode resolveField(FieldInsnNode instruction) {
		ClassNode classNode = context.resolveClass(instruction.owner);
		return resolveField(classNode, instruction.name, true);
	}

	private FieldNode resolveField(ClassNode classNode, String name, boolean allowPrivate) {
		for (FieldNode field : classNode.fields) {
			if (allowPrivate || (field.access & Opcodes.ACC_PRIVATE) == 0) {
				if (field.name.equals(name)) {
					return field;
				}
			}
		}
		if (classNode.superName == null) {
			throw new RuntimeException("could not resolve field " + name + " in class " + classNode.name);
		}
		return resolveField(context.resolveClass(classNode.superName), name, false);
	}

	private int getFieldWords(String descriptor) {
		return (descriptor.equals("J") || descriptor.equals("D")) ? 2 : 1;
	}

	private void writeGetstatic(FieldNode field) {
		int offset = ((FieldInfo)field).storageOffset;
		int words = getFieldWords(field.desc);
		if (words == 1) {
			out.println("	addi sp, sp, -4");
			out.println("	lw t0, staticFields + " + offset);
			out.println("	sw t0, 0(sp)");
		} else {
			out.println("	addi sp, sp, -8");
			out.println("	lw t0, staticFields + " + offset);
			out.println("	sw t0, 0(sp)");
			out.println("	lw t0, staticFields + " + (offset + 4));
			out.println("	sw t0, 4(sp)");
		}
	}

	private void writePutstatic(FieldNode field) {
		int offset = ((FieldInfo)field).storageOffset;
		int words = getFieldWords(field.desc);
		if (words == 1) {
			out.println("	lw t0, 0(sp)");
			out.println("	sw t0, staticFields + " + offset);
			out.println("	addi sp, sp, 4");
		} else {
			out.println("	lw t0, 0(sp)");
			out.println("	sw t0, staticFields + " + offset);
			out.println("	lw t0, 4(sp)");
			out.println("	sw t0, staticFields + " + (offset + 4));
			out.println("	addi sp, sp, 8");
		}
	}

	private void writeGetfield(FieldNode field) {
		int offset = ((FieldInfo)field).storageOffset;
		int words = getFieldWords(field.desc);
		if (words == 1) {
			out.println("	lw t1, 0(sp)");
			out.println("	lw t0, " + offset + "(t1)");
			out.println("	sw t0, 0(sp)");
		} else {
			out.println("	lw t1, 0(sp)");
			out.println("	addi sp, sp, -4");
			out.println("	lw t0, " + offset + "(t1)");
			out.println("	sw t0, 0(sp)");
			out.println("	lw t0, " + (offset + 4) + "(t1)");
			out.println("	sw t0, 4(sp)");
		}
	}

	private void writePutfield(FieldNode field) {
		int offset = ((FieldInfo)field).storageOffset;
		int words = getFieldWords(field.desc);
		if (words == 1) {
			out.println("	lw t1, 4(sp)");
			out.println("	lw t0, 0(sp)");
			out.println("	sw t0, " + offset + "(t1)");
			out.println("	addi sp, sp, 8");
		} else {
			out.println("	lw t1, 8(sp)");
			out.println("	lw t0, 0(sp)");
			out.println("	sw t0, " + offset + "(t1)");
			out.println("	lw t0, 4(sp)");
			out.println("	sw t0, " + (offset + 4) + "(t1)");
			out.println("	addi sp, sp, 12");
		}
	}

	private void writeArrayLoad(int indexShiftAmount, boolean doubleword, String loadInstruction) {
		out.println("	lw t0, 4(sp)");
		out.println("	lw t1, 0(sp)");
		out.println("	sll t1, t1, " + indexShiftAmount);
		out.println("	add t0, t0, t1");
		if (doubleword) {
			out.println("	lw t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("	lw t3, " + (context.getArrayHeaderSize() + 4) + "(t0)");
			out.println("	sw t2, 0(sp)");
			out.println("	sw t3, 4(sp)");
		} else {
			out.println("	" + loadInstruction + " t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("	addi sp, sp, 4");
			out.println("	sw t2, 0(sp)");
		}
	}

	private void writeArrayStore(int indexShiftAmount, boolean doubleword, String storeInstruction) {
		if (doubleword) {
			out.println("	lw t0, 12(sp)");
			out.println("	lw t1, 8(sp)");
			out.println("	lw t2, 4(sp)");
			out.println("	lw t3, 0(sp)");
			out.println("	addi sp, sp, 16");
		} else {
			out.println("	lw t0, 8(sp)");
			out.println("	lw t1, 4(sp)");
			out.println("	lw t2, 0(sp)");
			out.println("	addi sp, sp, 12");
		}
		out.println("	sll t1, t1, " + indexShiftAmount);
		out.println("	add t0, t0, t1");
		if (doubleword) {
			out.println("	sw t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("	sw t3, " + (context.getArrayHeaderSize() + 4) + "(t0)");
		} else {
			out.println("	" + storeInstruction + " t2, " + context.getArrayHeaderSize() + "(t0)");
		}
	}

	public interface Context {
		ClassInfo resolveClass(String name);
		int getArrayHeaderSize();
		String getRuntimeObjectLabel(Object o);
	}

}
