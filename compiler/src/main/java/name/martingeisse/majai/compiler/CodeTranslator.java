package name.martingeisse.majai.compiler;

import name.martingeisse.majai.compiler.descriptor.ParsedMethodDescriptor;
import name.martingeisse.majai.vm.VmObjectArrayMetadata;
import name.martingeisse.majai.vm.VmObjectMetadata;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class CodeTranslator {

	private final Context context;
	private final PrintWriter out;
	private final MethodInfo methodInfo;
	private final String mangledMethodName;
	private final String returnLabel;
	private final List<Label> internalLabels = new ArrayList<>();

	CodeTranslator(Context context, PrintWriter out, MethodInfo methodInfo) {
		this.context = context;
		this.out = out;
		this.methodInfo = methodInfo;
		this.mangledMethodName = NameUtil.mangleMethodName(methodInfo);
		this.returnLabel = mangledMethodName + "__return";
	}

	void translate() {
		if ((methodInfo.access & Opcodes.ACC_NATIVE) != 0) {
			return;
		}

		// intro
		out.println(mangledMethodName + ':');
		out.println("\taddi sp, sp, -" + (methodInfo.maxLocals + 2) * 4);
		out.println("\tsw ra, " + ((methodInfo.maxLocals) * 4) + "(sp)");
		out.println("\tsw s0, " + ((methodInfo.maxLocals + 1) * 4) + "(sp)");
		out.println("\tmv s0, sp");
		{
			int words = (methodInfo.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
			words += methodInfo.parsedDescriptor.getParameterWords();
			for (int i = 0; i < words; i++) {
				out.println("\tsw a" + i + ", " + (i * 4) + "(s0)");
			}
		}

		// code
		{
			for (AbstractInsnNode instruction = methodInfo.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
				if (instruction instanceof LabelNode) {
					internalLabels.add(((LabelNode) instruction).getLabel());
				}
			}
			for (AbstractInsnNode instruction = methodInfo.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
				translate(instruction);
			}
		}

		// outro
		out.println(returnLabel + ':');
		out.println("\tlw s0, " + ((methodInfo.maxLocals + 1) * 4) + "(sp)");
		out.println("\tlw ra, " + ((methodInfo.maxLocals) * 4) + "(sp)");
		out.println("\tadd sp, sp, " + (methodInfo.maxLocals + 2) * 4);
		out.println("\tret");
		out.println("");

	}

	private void translate(AbstractInsnNode instruction) {
		int opcode = instruction.getOpcode();
		switch (opcode) {

			case -1:
				if (instruction instanceof LabelNode) {
					out.println(getLabelName((LabelNode) instruction) + ':');
				} else if (instruction instanceof FrameNode) {
					// not needed yet
				} else {
					throw new RuntimeException("unknown opcode-less instruction node: " + instruction);
				}
				break;

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
				} else if (ldc.cst instanceof Float) {
					throw new NotYetImplementedException();
				} else if (ldc.cst instanceof Long) {
					throw new NotYetImplementedException();
				} else if (ldc.cst instanceof Double) {
					throw new NotYetImplementedException();
				} else if (ldc.cst instanceof String) {
					String label = context.getRuntimeObjectLabel(ldc.cst);
					out.println("\tla t0, " + label);
					push("t0");
				} else if (ldc.cst instanceof Type) {
					throw new NotYetImplementedException();
				} else {
					throw new NotYetImplementedException("ldc with invalid constant type: " + ldc.cst.getClass());
				}
				break;
			}

			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.ALOAD:
				//noinspection ConstantConditions
				load32(((VarInsnNode) instruction).var);
				break;

			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
				//noinspection ConstantConditions
				load64(((VarInsnNode) instruction).var);
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
				store32(((VarInsnNode) instruction).var);
				break;

			case Opcodes.LSTORE:
			case Opcodes.DSTORE:
				//noinspection ConstantConditions
				store32(((VarInsnNode) instruction).var);
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
				out.println("\tadd sp, sp, 4");
				break;

			case Opcodes.POP2:
				out.println("\tadd sp, sp, 8");
				break;

			case Opcodes.DUP:
				out.println("\tlw t0, 0(sp)");
				out.println("\taddi sp, sp, -4");
				out.println("\tsw t0, 0(sp)");
				break;

			case Opcodes.DUP_X1:
				out.println("\tlw t0, 0(sp)");
				out.println("\tlw t1, 4(sp)");
				out.println("\taddi sp, sp, -4");
				out.println("\tsw t0, 0(sp)");
				out.println("\tsw t1, 4(sp)");
				out.println("\tsw t0, 8(sp)");
				break;

			case Opcodes.DUP_X2:
				out.println("\tlw t0, 0(sp)");
				out.println("\tlw t1, 4(sp)");
				out.println("\tlw t2, 8(sp)");
				out.println("\taddi sp, sp, -4");
				out.println("\tsw t0, 0(sp)");
				out.println("\tsw t1, 4(sp)");
				out.println("\tsw t2, 8(sp)");
				out.println("\tsw t0, 0(sp)");
				break;

			case Opcodes.DUP2_X1:
				out.println("\tlw t0, 0(sp)");
				out.println("\tlw t1, 4(sp)");
				out.println("\tlw t2, 8(sp)");
				out.println("\taddi sp, sp, -4");
				out.println("\tsw t0, 0(sp)");
				out.println("\tsw t1, 4(sp)");
				out.println("\tsw t2, 8(sp)");
				out.println("\tsw t0, 12(sp)");
				out.println("\tsw t1, 16(sp)");
				break;

			case Opcodes.DUP2_X2:
				out.println("\tlw t0, 0(sp)");
				out.println("\tlw t1, 4(sp)");
				out.println("\tlw t2, 8(sp)");
				out.println("\tlw t3, 8(sp)");
				out.println("\taddi sp, sp, -8");
				out.println("\tsw t0, 0(sp)");
				out.println("\tsw t1, 4(sp)");
				out.println("\tsw t2, 8(sp)");
				out.println("\tsw t3, 12(sp)");
				out.println("\tsw t0, 16(sp)");
				out.println("\tsw t1, 20(sp)");
				break;

			case Opcodes.SWAP:
				out.println("\tlw t0, 0(sp)");
				out.println("\tlw t1, 4(sp)");
				out.println("\tsw t1, 0(sp)");
				out.println("\tsw t0, 4(sp)");
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
				IincInsnNode inc = (IincInsnNode) instruction;
				out.println("\tlw t0, " + (inc.var * 4) + "(s0)");
				out.println("\taddi t0, t0, " + inc.incr);
				out.println("\tsw t0, " + (inc.var * 4) + "(s0)");
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
				out.println("\tlb t0, 0(sp)");
				out.println("\tsw t0, 0(sp)");
				break;

			case Opcodes.I2C:
				out.println("\tlhu t0, 0(sp)");
				out.println("\tsw t0, 0(sp)");
				break;

			case Opcodes.I2S:
				out.println("\tlh t0, 0(sp)");
				out.println("\tsw t0, 0(sp)");
				break;

			case Opcodes.LCMP:
			case Opcodes.FCMPL:
			case Opcodes.FCMPG:
			case Opcodes.DCMPL:
			case Opcodes.DCMPG:
				throw new NotYetImplementedException();

			case Opcodes.IFEQ:
				branch((JumpInsnNode) instruction, "beq", true);
				break;

			case Opcodes.IFNE:
				branch((JumpInsnNode) instruction, "bne", true);
				break;

			case Opcodes.IFLT:
				branch((JumpInsnNode) instruction, "blt", true);
				break;

			case Opcodes.IFGE:
				branch((JumpInsnNode) instruction, "bge", true);
				break;

			case Opcodes.IFGT:
				branch((JumpInsnNode) instruction, "bgt", true);
				break;

			case Opcodes.IFLE:
				branch((JumpInsnNode) instruction, "ble", true);
				break;

			case Opcodes.IF_ICMPEQ:
				branch((JumpInsnNode) instruction, "beq", false);
				break;

			case Opcodes.IF_ICMPNE:
				branch((JumpInsnNode) instruction, "bne", false);
				break;

			case Opcodes.IF_ICMPLT:
				branch((JumpInsnNode) instruction, "blt", false);
				break;

			case Opcodes.IF_ICMPGE:
				branch((JumpInsnNode) instruction, "bge", false);
				break;

			case Opcodes.IF_ICMPGT:
				branch((JumpInsnNode) instruction, "bgt", false);
				break;

			case Opcodes.IF_ICMPLE:
				branch((JumpInsnNode) instruction, "ble", false);
				break;

			case Opcodes.IF_ACMPEQ:
				branch((JumpInsnNode) instruction, "beq", false);
				break;

			case Opcodes.IF_ACMPNE:
				branch((JumpInsnNode) instruction, "bne", false);
				break;

			case Opcodes.GOTO:
				out.println("\tj " + getLabelName((JumpInsnNode) instruction));
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
				out.println("\tj " + returnLabel);
				break;

			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
				pop("a0");
				pop("a1");
				out.println("\tj " + returnLabel);
				break;

			case Opcodes.RETURN:
				out.println("\tj " + returnLabel);
				break;

			case Opcodes.GETSTATIC: {
				writeGetstatic((FieldInfo) resolveField((FieldInsnNode) instruction));
				break;
			}

			case Opcodes.PUTSTATIC: {
				writePutstatic((FieldInfo) resolveField((FieldInsnNode) instruction));
				break;
			}

			case Opcodes.GETFIELD: {
				writeGetfield((FieldInfo) resolveField((FieldInsnNode) instruction));
				break;
			}

			case Opcodes.PUTFIELD: {
				writePutfield((FieldInfo) resolveField((FieldInsnNode) instruction));
				break;
			}

			case Opcodes.INVOKEVIRTUAL:
				invokevirtual((MethodInsnNode) instruction);
				break;

			case Opcodes.INVOKESPECIAL:
				invokenonvirtual((MethodInsnNode) instruction, false);
				break;

			case Opcodes.INVOKESTATIC:
				invokenonvirtual((MethodInsnNode) instruction, true);
				break;

			case Opcodes.INVOKEINTERFACE:
			case Opcodes.INVOKEDYNAMIC:
				throw new NotYetImplementedException();

			case Opcodes.NEW: {
				TypeInsnNode typeInstruction = (TypeInsnNode) instruction;
				String className = typeInstruction.desc;
				ClassInfo classInfo = context.resolveClass(className);
				out.println("\tli a0, " + classInfo.fieldAllocator.getWordCount());
				out.println("\tla a1, " + NameUtil.mangleClassName(classInfo) + "_vtable");
				out.println("\tcall allocateMemory");
				push("a0");
				break;
			}

			case Opcodes.NEWARRAY: {
				pop("a0");
				int elementTypeCode = ((IntInsnNode) instruction).operand;
				VmObjectMetadata metadata;
				int shiftAmount;
				switch (elementTypeCode) {

					case 4: // boolean
						metadata = context.resolveObjectMetadata("[Z");
						shiftAmount = 0;
						break;

					case 8: // byte
						metadata = context.resolveObjectMetadata("[B");
						shiftAmount = 0;
						break;

					case 9: // short
						metadata = context.resolveObjectMetadata("[S");
						shiftAmount = 1;
						break;

					case 5: // char
						metadata = context.resolveObjectMetadata("[C");
						shiftAmount = 1;
						break;

					case 10: // int
						metadata = context.resolveObjectMetadata("[I");
						shiftAmount = 2;
						break;

					case 6: // float
						metadata = context.resolveObjectMetadata("[F");
						shiftAmount = 2;
						break;

					case 11: // long
						metadata = context.resolveObjectMetadata("[J");
						shiftAmount = 3;
						break;

					case 7: // double
						metadata = context.resolveObjectMetadata("[D");
						shiftAmount = 3;
						break;

					default:
						throw new RuntimeException("invalid newarray element type code: " + elementTypeCode);

				}
				out.println("\tsll a0, a0, " + shiftAmount);
				out.println("\tadd a0, a0, " + context.getArrayHeaderSize());
				out.println("\tla a1, " + context.getRuntimeObjectLabel(metadata.getVtable()));
				out.println("\tcall allocateMemory");
				push("a0");
				break;
			}

			case Opcodes.ANEWARRAY: {
				String elementDesc = ((TypeInsnNode) instruction).desc;
				String arrayDesc = "[" + elementDesc;
				VmObjectArrayMetadata metadata = (VmObjectArrayMetadata) context.resolveObjectMetadata(arrayDesc);
				pop("a0");
				out.println("\tsll a0, a0, 2");
				out.println("\tadd a0, a0, " + context.getArrayHeaderSize());
				out.println("\tla a1, " + context.getRuntimeObjectLabel(metadata.getVtable()));
				out.println("\tcall allocateMemory");
				push("a0");
				break;
			}

			case Opcodes.ARRAYLENGTH:
				writeGetfield(context.getArrayHeaderSize() - 4, 1, "lw");
				break;

			case Opcodes.ATHROW:
			case Opcodes.CHECKCAST:
			case Opcodes.INSTANCEOF:
			case Opcodes.MONITORENTER:
			case Opcodes.MONITOREXIT:
			case Opcodes.MULTIANEWARRAY:
				throw new NotYetImplementedException();

			case Opcodes.IFNULL:
				branch((JumpInsnNode) instruction, "beq", true);
				break;

			case Opcodes.IFNONNULL:
				branch((JumpInsnNode) instruction, "bne", true);
				break;

			default:
				throw new RuntimeException("unknown opcode: " + opcode);

		}
	}

//region stack manipulation and computation

	private void pushInt(int value) {
		out.println("\tli t0, " + value);
		push("t0");
	}

	private void pushLong(long value) {
		pushInt((int) (value >> 32));
		pushInt((int) value);
	}

	private void load32(int index) {
		out.println("\tlw t0, " + (index * 4) + "(s0)");
		push("t0");
	}

	private void store32(int index) {
		pop("t0");
		out.println("\tsw t0, " + (index * 4) + "(s0)");
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
		out.println("\taddi sp, sp, -4");
		out.println("\tsw " + register + ", 0(sp)");
	}

	private void pop(String register) {
		out.println("\tlw " + register + ", 0(sp)");
		out.println("\tadd sp, sp, 4");
	}

	private void peek(String register) {
		out.println("\tlw " + register + ", 0(sp)");
	}

	private void wordOp(String instruction) {
		out.println("\tlw t0, 4(sp)");
		out.println("\tlw t1, 0(sp)");
		out.println("\t" + instruction + " t0, t0, t1");
		out.println("\tadd sp, sp, 4");
		out.println("\tsw t0, 0(sp)");
	}

//endregion

//region control transfer

	private void branch(JumpInsnNode bytecodeInstruction, String machineInstruction, boolean implicitZero) {
		if (!implicitZero) {
			pop("t1");
		}
		pop("t0");
		out.println("\t" + machineInstruction + " t0, " + (implicitZero ? "x0" : "t1") + ", " + getLabelName(bytecodeInstruction));
	}

	private int getLabelIndex(Label label) {
		int index = internalLabels.indexOf(label);
		if (index < 0) {
			throw new RuntimeException("referring to unknown internal label");
		}
		return index;
	}

	private String getLabelName(Label label) {
		return mangledMethodName + '_' + getLabelIndex(label);
	}

	private String getLabelName(LabelNode labelNode) {
		return getLabelName(labelNode.getLabel());
	}

	private String getLabelName(JumpInsnNode jump) {
		return getLabelName(jump.label);
	}

//endregion

//region field access

	private FieldNode resolveField(FieldInsnNode instruction) {
		ClassNode classNode = context.resolveClass(instruction.owner);
		return resolveField(classNode, instruction.name, true);
	}

	private FieldInfo resolveField(ClassNode classNode, String name, boolean allowPrivate) {
		for (FieldNode field : classNode.fields) {
			if (allowPrivate || (field.access & Opcodes.ACC_PRIVATE) == 0) {
				if (field.name.equals(name)) {
					return (FieldInfo) field;
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

	private void writeGetstatic(FieldInfo field) {
		writeGetstatic(field.storageOffset, getFieldWords(field.desc), field.parsedDescriptor.getLoadInstruction());
	}

	private void writeGetstatic(int offset, int words, String loadInstruction) {
		if (words == 1) {
			out.println("\taddi sp, sp, -4");
			out.println("\t" + loadInstruction + " t0, staticFields + " + offset);
			out.println("\tsw t0, 0(sp)");
		} else {
			out.println("\taddi sp, sp, -8");
			out.println("\tlw t0, staticFields + " + offset);
			out.println("\tsw t0, 0(sp)");
			out.println("\tlw t0, staticFields + " + (offset + 4));
			out.println("\tsw t0, 4(sp)");
		}
	}

	private void writePutstatic(FieldInfo field) {
		writePutstatic(field.storageOffset, getFieldWords(field.desc), field.parsedDescriptor.getStoreInstruction());
	}

	private void writePutstatic(int offset, int words, String storeInstruction) {
		if (words == 1) {
			out.println("\tlw t0, 0(sp)");
			out.println("\t" + storeInstruction + " t0, staticFields + " + offset);
			out.println("\taddi sp, sp, 4");
		} else {
			out.println("\tlw t0, 0(sp)");
			out.println("\tsw t0, staticFields + " + offset);
			out.println("\tlw t0, 4(sp)");
			out.println("\tsw t0, staticFields + " + (offset + 4));
			out.println("\taddi sp, sp, 8");
		}
	}

	private void writeGetfield(FieldInfo field) {
		writeGetfield(field.storageOffset, getFieldWords(field.desc), field.parsedDescriptor.getLoadInstruction());
	}

	private void writeGetfield(int offset, int words, String loadInstruction) {
		if (words == 1) {
			out.println("\tlw t1, 0(sp)");
			out.println("\t" + loadInstruction + " t0, " + offset + "(t1)");
			out.println("\tsw t0, 0(sp)");
		} else {
			out.println("\tlw t1, 0(sp)");
			out.println("\taddi sp, sp, -4");
			out.println("\tlw t0, " + offset + "(t1)");
			out.println("\tsw t0, 0(sp)");
			out.println("\tlw t0, " + (offset + 4) + "(t1)");
			out.println("\tsw t0, 4(sp)");
		}
	}

	private void writePutfield(FieldInfo field) {
		writePutfield(field.storageOffset, getFieldWords(field.desc), field.parsedDescriptor.getStoreInstruction());
	}

	private void writePutfield(int offset, int words, String storeInstruction) {
		if (words == 1) {
			out.println("\tlw t1, 4(sp)");
			out.println("\t" + storeInstruction + " t0, 0(sp)");
			out.println("\tsw t0, " + offset + "(t1)");
			out.println("\taddi sp, sp, 8");
		} else {
			out.println("\tlw t1, 8(sp)");
			out.println("\tlw t0, 0(sp)");
			out.println("\tsw t0, " + offset + "(t1)");
			out.println("\tlw t0, 4(sp)");
			out.println("\tsw t0, " + (offset + 4) + "(t1)");
			out.println("\taddi sp, sp, 12");
		}
	}

//endregion

//region array element access

	private void writeArrayLoad(int indexShiftAmount, boolean doubleword, String loadInstruction) {
		out.println("\tlw t0, 4(sp)");
		out.println("\tlw t1, 0(sp)");
		out.println("\tsll t1, t1, " + indexShiftAmount);
		out.println("\tadd t0, t0, t1");
		if (doubleword) {
			out.println("\tlw t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("\tlw t3, " + (context.getArrayHeaderSize() + 4) + "(t0)");
			out.println("\tsw t2, 0(sp)");
			out.println("\tsw t3, 4(sp)");
		} else {
			out.println("\t" + loadInstruction + " t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("\taddi sp, sp, 4");
			out.println("\tsw t2, 0(sp)");
		}
	}

	private void writeArrayStore(int indexShiftAmount, boolean doubleword, String storeInstruction) {
		if (doubleword) {
			out.println("\tlw t0, 12(sp)");
			out.println("\tlw t1, 8(sp)");
			out.println("\tlw t2, 4(sp)");
			out.println("\tlw t3, 0(sp)");
			out.println("\taddi sp, sp, 16");
		} else {
			out.println("\tlw t0, 8(sp)");
			out.println("\tlw t1, 4(sp)");
			out.println("\tlw t2, 0(sp)");
			out.println("\taddi sp, sp, 12");
		}
		out.println("\tsll t1, t1, " + indexShiftAmount);
		out.println("\tadd t0, t0, t1");
		if (doubleword) {
			out.println("\tsw t2, " + context.getArrayHeaderSize() + "(t0)");
			out.println("\tsw t3, " + (context.getArrayHeaderSize() + 4) + "(t0)");
		} else {
			out.println("\t" + storeInstruction + " t2, " + context.getArrayHeaderSize() + "(t0)");
		}
	}

//endregion

//region method invocation

	private void invokenonvirtual(MethodInsnNode call, boolean staticMethod) {
		ParsedMethodDescriptor parsedMethodDescriptor = new ParsedMethodDescriptor(call.desc);
		int effectiveParameterWords = parsedMethodDescriptor.getParameterWords() + (staticMethod ? 0 : 1);
		for (int i = 0; i < effectiveParameterWords; i++) {
			out.println("\tlw a" + i + ", " + (4 * (effectiveParameterWords - 1 - i)) + "(sp)");
		}
		out.println("\taddi sp, sp, " + (4 * effectiveParameterWords));
		out.println("\tcall " + NameUtil.mangleMethodName(call));
		if (parsedMethodDescriptor.getReturnWords() == 1) {
			out.println("\taddi sp, sp, -4");
			out.println("\tsw a0, 0(sp)");
		} else if (parsedMethodDescriptor.getReturnWords() == 2) {
			out.println("\taddi sp, sp, -8");
			out.println("\tsw a0, 0(sp)");
			out.println("\tsw a1, 4(sp)");
		}
	}

	private void invokevirtual(MethodInsnNode call) {

		// find vtable index
		ClassInfo targetClassInfo = context.resolveClass(call.owner);
		int vtableIndex = targetClassInfo.vtableAllocator.findByName(call.name).vtableIndex;

		// move arguments from the stack to a* registers
		ParsedMethodDescriptor parsedMethodDescriptor = new ParsedMethodDescriptor(call.desc);
		int effectiveParameterWords = parsedMethodDescriptor.getParameterWords() + 1;
		for (int i = 0; i < effectiveParameterWords; i++) {
			out.println("\tlw a" + i + ", " + (4 * (effectiveParameterWords - 1 - i)) + "(sp)");
		}
		out.println("\taddi sp, sp, " + (4 * effectiveParameterWords));

		// invoke the method
		out.println("\tlw t0, 0(a0)"); // load pointer to vtable from the object
		out.println("\tlw t0, " + (context.getArrayHeaderSize() + 4 * vtableIndex) + "(t0)"); // load pointer to code from the vtable
		out.println("\tjalr t0"); // invoke the method

		// move return values from a* registers to the stack
		if (parsedMethodDescriptor.getReturnWords() == 1) {
			out.println("\taddi sp, sp, -4");
			out.println("\tsw a0, 0(sp)");
		} else if (parsedMethodDescriptor.getReturnWords() == 2) {
			out.println("\taddi sp, sp, -8");
			out.println("\tsw a0, 0(sp)");
			out.println("\tsw a1, 4(sp)");
		}

	}

//endregion

	public interface Context {
		ClassInfo resolveClass(String name);

		VmObjectMetadata resolveObjectMetadata(String name);

		int getArrayHeaderSize();

		String getRuntimeObjectLabel(Object o);
	}

}
