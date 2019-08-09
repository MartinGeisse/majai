package name.martingeisse.majai;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

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

				case Opcodes.IALOAD:
				case Opcodes.LALOAD:
				case Opcodes.FALOAD:
				case Opcodes.DALOAD:
				case Opcodes.AALOAD:
				case Opcodes.BALOAD:
				case Opcodes.CALOAD:
				case Opcodes.SALOAD:
					throw new NotYetImplementedException();

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
				case Opcodes.LASTORE:
				case Opcodes.FASTORE:
				case Opcodes.DASTORE:
				case Opcodes.AASTORE:
				case Opcodes.BASTORE:
				case Opcodes.CASTORE:
				case Opcodes.SASTORE:
					throw new NotYetImplementedException();

				case Opcodes.POP:
					out.println("	add sp, sp, 4");
					break;

				case Opcodes.POP2:
					out.println("	add sp, sp, 8");
					break;

				case Opcodes.DUP:
					out.println("	lw t0, 0(sp)");
					out.println("	sub sp, sp, 4");
					out.println("	sw t0, 0(sp)");
					break;

				case Opcodes.DUP_X1:
					out.println("	lw t0, 0(sp)");
					out.println("	lw t1, 4(sp)");
					out.println("	sub sp, sp, 4");
					out.println("	sw t0, 0(sp)");
					out.println("	sw t1, 4(sp)");
					out.println("	sw t0, 8(sp)");
					break;

				case Opcodes.DUP_X2:
					out.println("	lw t0, 0(sp)");
					out.println("	lw t1, 4(sp)");
					out.println("	lw t2, 8(sp)");
					out.println("	sub sp, sp, 4");
					out.println("	sw t0, 0(sp)");
					out.println("	sw t1, 4(sp)");
					out.println("	sw t2, 8(sp)");
					out.println("	sw t0, 0(sp)");
					break;

				case Opcodes.DUP2_X1:
					out.println("	lw t0, 0(sp)");
					out.println("	lw t1, 4(sp)");
					out.println("	lw t2, 8(sp)");
					out.println("	sub sp, sp, 4");
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
					out.println("	sub sp, sp, 8");
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
					TODO;
					break;

				case Opcodes.I2C:
					TODO;
					break;

				case Opcodes.I2S:
					TODO;
					break;

				case Opcodes.LCMP:
				case Opcodes.FCMPL:
				case Opcodes.FCMPG:
				case Opcodes.DCMPL:
				case Opcodes.DCMPG:
					throw new NotYetImplementedException();

				case Opcodes.IFEQ: // visitJumpInsn
					TODO;
					break;

				case Opcodes.IFNE:
					TODO;
					break;

				case Opcodes.IFLT:
					TODO;
					break;

				case Opcodes.IFGE:
					TODO;
					break;

				case Opcodes.IFGT:
					TODO;
					break;

				case Opcodes.IFLE:
					TODO;
					break;

				case Opcodes.IF_ICMPEQ:
					TODO;
					break;

				case Opcodes.IF_ICMPNE:
					TODO;
					break;

				case Opcodes.IF_ICMPLT:
					TODO;
					break;

				case Opcodes.IF_ICMPGE:
					TODO;
					break;

				case Opcodes.IF_ICMPGT:
					TODO;
					break;

				case Opcodes.IF_ICMPLE:
					TODO;
					break;

				case Opcodes.IF_ACMPEQ:
					TODO;
					break;

				case Opcodes.IF_ACMPNE:
					TODO;
					break;

				case Opcodes.GOTO:
					TODO;
					break;

				case Opcodes.JSR:
				case Opcodes.RET:
				case Opcodes.TABLESWITCH:
				case Opcodes.LOOKUPSWITCH:
					throw new NotYetImplementedException();

				case Opcodes.IRETURN:
				case Opcodes.FRETURN:
				case Opcodes.ARETURN:
					TODO;
					break;

				case Opcodes.LRETURN:
				case Opcodes.DRETURN:
					TODO;
					break;

				case Opcodes.RETURN:
					TODO;
					break;

				case Opcodes.GETSTATIC:
				case Opcodes.PUTSTATIC:
				case Opcodes.GETFIELD:
				case Opcodes.PUTFIELD:
				case Opcodes.INVOKEVIRTUAL:
				case Opcodes.INVOKESPECIAL:
				case Opcodes.INVOKESTATIC:
				case Opcodes.INVOKEINTERFACE:
				case Opcodes.INVOKEDYNAMIC:
				case Opcodes.NEW:
				case Opcodes.NEWARRAY:
				case Opcodes.ANEWARRAY:
				case Opcodes.ARRAYLENGTH:
				case Opcodes.ATHROW:
				case Opcodes.CHECKCAST:
				case Opcodes.INSTANCEOF:
				case Opcodes.MONITORENTER:
				case Opcodes.MONITOREXIT:
				case Opcodes.MULTIANEWARRAY:
					throw new NotYetImplementedException();

				case Opcodes.IFNULL:
					TODO;
					break;

				case Opcodes.IFNONNULL:
					TODO;
					break;

				default:
					throw new RuntimeException("unknown opcode: " + opcode);

			}

		}
		out.println("	// " + instruction);
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
		out.println("	sub sp, sp, 4");
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

}
