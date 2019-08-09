package name.martingeisse.majai.payload;

/**
 *
 */
public class Test {

	static native void storeWord(int address, int value);
	static native void print(String s);

	public static int test(int x) {
		return x + x + 1;
	}

	public static int foo(int x) {
		return test(test(x));
	}

	public static void main() {
		storeWord(0x8000_0000, 'Z');
	}

}
