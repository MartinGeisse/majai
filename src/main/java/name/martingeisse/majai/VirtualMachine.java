package name.martingeisse.majai;

/**
 *
 */
public class VirtualMachine {

	public static final int OPCODE_EXIT = 0;
	public static final int OPCODE_INT32 = 1;
	public static final int OPCODE_ADD = 2;
	public static final int OPCODE_SUB = 3;
	public static final int OPCODE_DUP = 4;
	public static final int OPCODE_WRITE = 5;

	private final byte[] program;
	private int programCounter = 0;

	private final int[] stack = new int[1024 * 1024];
	private int stackSize = 0;

	private final int[] heap = new int[16 * 1024 * 1024];

	private boolean finished = false;

	public VirtualMachine(byte[] program) {
		this.program = program;
	}

	public void run() {
		while (!finished) {
			step();
		}
	}

	public void step() {
		int opcode = fetchUnsignedProgramByte();
		switch (opcode) {

			case OPCODE_EXIT: {
				finished = true;
				break;
			}

			case OPCODE_INT32: {
				int value = fetchSignedProgramByte();
				value = (value << 8) + fetchUnsignedProgramByte();
				value = (value << 8) + fetchUnsignedProgramByte();
				value = (value << 8) + fetchUnsignedProgramByte();
				push(value);
				break;
			}

			case OPCODE_ADD: {
				push(pop() + pop());
				break;
			}

			case OPCODE_SUB: {
				push(-pop() + pop());
				break;
			}

			case OPCODE_DUP: {
				int value = pop();
				push(value);
				push(value);
				break;
			}


			case OPCODE_WRITE: {
				System.out.print(pop());
				break;
			}

			default:
				throw new RuntimeException("illegal opcode " + opcode + " at " + (programCounter - 1));

		}
	}

	private int fetchUnsignedProgramByte() {
		int result = program[programCounter] & 0xff;
		programCounter++;
		return result;
	}

	private int fetchSignedProgramByte() {
		int result = program[programCounter];
		programCounter++;
		return result;
	}

	private void push(int value) {
		stack[stackSize] = value;
		stackSize++;
	}

	private int pop() {
		stackSize--;
		return stack[stackSize];
	}

}
