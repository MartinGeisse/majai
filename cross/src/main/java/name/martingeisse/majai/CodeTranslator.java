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

				/*

  int POP = 87; // -
  int POP2 = 88; // -
  int DUP = 89; // -
  int DUP_X1 = 90; // -
  int DUP_X2 = 91; // -
  int DUP2 = 92; // -
  int DUP2_X1 = 93; // -
  int DUP2_X2 = 94; // -
  int SWAP = 95; // -
  int IADD = 96; // -
  int LADD = 97; // -
  int FADD = 98; // -
  int DADD = 99; // -
  int ISUB = 100; // -
  int LSUB = 101; // -
  int FSUB = 102; // -
  int DSUB = 103; // -
  int IMUL = 104; // -
  int LMUL = 105; // -
  int FMUL = 106; // -
  int DMUL = 107; // -
  int IDIV = 108; // -
  int LDIV = 109; // -
  int FDIV = 110; // -
  int DDIV = 111; // -
  int IREM = 112; // -
  int LREM = 113; // -
  int FREM = 114; // -
  int DREM = 115; // -
  int INEG = 116; // -
  int LNEG = 117; // -
  int FNEG = 118; // -
  int DNEG = 119; // -
  int ISHL = 120; // -
  int LSHL = 121; // -
  int ISHR = 122; // -
  int LSHR = 123; // -
  int IUSHR = 124; // -
  int LUSHR = 125; // -
  int IAND = 126; // -
  int LAND = 127; // -
  int IOR = 128; // -
  int LOR = 129; // -
  int IXOR = 130; // -
  int LXOR = 131; // -
  int IINC = 132; // visitIincInsn
  int I2L = 133; // visitInsn
  int I2F = 134; // -
  int I2D = 135; // -
  int L2I = 136; // -
  int L2F = 137; // -
  int L2D = 138; // -
  int F2I = 139; // -
  int F2L = 140; // -
  int F2D = 141; // -
  int D2I = 142; // -
  int D2L = 143; // -
  int D2F = 144; // -
  int I2B = 145; // -
  int I2C = 146; // -
  int I2S = 147; // -
  int LCMP = 148; // -
  int FCMPL = 149; // -
  int FCMPG = 150; // -
  int DCMPL = 151; // -
  int DCMPG = 152; // -
  int IFEQ = 153; // visitJumpInsn
  int IFNE = 154; // -
  int IFLT = 155; // -
  int IFGE = 156; // -
  int IFGT = 157; // -
  int IFLE = 158; // -
  int IF_ICMPEQ = 159; // -
  int IF_ICMPNE = 160; // -
  int IF_ICMPLT = 161; // -
  int IF_ICMPGE = 162; // -
  int IF_ICMPGT = 163; // -
  int IF_ICMPLE = 164; // -
  int IF_ACMPEQ = 165; // -
  int IF_ACMPNE = 166; // -
  int GOTO = 167; // -
  int JSR = 168; // -
  int RET = 169; // visitVarInsn
  int TABLESWITCH = 170; // visiTableSwitchInsn
  int LOOKUPSWITCH = 171; // visitLookupSwitch
  int IRETURN = 172; // visitInsn
  int LRETURN = 173; // -
  int FRETURN = 174; // -
  int DRETURN = 175; // -
  int ARETURN = 176; // -
  int RETURN = 177; // -
  int GETSTATIC = 178; // visitFieldInsn
  int PUTSTATIC = 179; // -
  int GETFIELD = 180; // -
  int PUTFIELD = 181; // -
  int INVOKEVIRTUAL = 182; // visitMethodInsn
  int INVOKESPECIAL = 183; // -
  int INVOKESTATIC = 184; // -
  int INVOKEINTERFACE = 185; // -
  int INVOKEDYNAMIC = 186; // visitInvokeDynamicInsn
  int NEW = 187; // visitTypeInsn
  int NEWARRAY = 188; // visitIntInsn
  int ANEWARRAY = 189; // visitTypeInsn
  int ARRAYLENGTH = 190; // visitInsn
  int ATHROW = 191; // -
  int CHECKCAST = 192; // visitTypeInsn
  int INSTANCEOF = 193; // -
  int MONITORENTER = 194; // visitInsn
  int MONITOREXIT = 195; // -
  int MULTIANEWARRAY = 197; // visitMultiANewArrayInsn
  int IFNULL = 198; // visitJumpInsn
  int IFNONNULL = 199; // -

				 */

			}

		}
		out.println("	// " + instruction);
	}

	private void pushInt(int value) {
		out.println("	li s1, " + value);
		push("s1");
	}

	private void pushLong(long value) {
		pushInt((int) (value >> 32));
		pushInt((int) value);
	}

	private void load32(int index) {
		out.println("	lw s1, " + (index * 4) + "(s0)");
		push("s1");
	}

	private void store32(int index) {
		pop("s1");
		out.println("	sw s1, " + (index * 4) + "(s0)");
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

}
