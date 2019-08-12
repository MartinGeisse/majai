package name.martingeisse.majai.payload;

/**
 *
 */
public class Test {

	public static int TEST_STATIC_FIELD_1;
	public static int TEST_STATIC_FIELD_2;
	public static int TEST_STATIC_FIELD_3;

	static native void storeWord(int address, int value);
	static native void print(String s);

	public static int test(int x) {
		return x + x + 1;
	}

	public static int foo(int x) {
		return test(test(x));
	}

	public static int getConstant1() {
		return TEST_STATIC_FIELD_1;
	}

	public static int getConstant2() {
		return TEST_STATIC_FIELD_2;
	}

	public static int getConstant3() {
		return TEST_STATIC_FIELD_3;
	}

	public static Object allocate() {
		return new Object();
	}

	public static void main() {
		storeWord(0x8000_0000, 'Z');
	}

}
