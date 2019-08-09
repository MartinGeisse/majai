package name.martingeisse.majai.payload;

/**
 *
 */
public class Test {

	static native void print(String s);

	public static int test(int x) {
		return x + x + 1;
	}

	public static int foo(int x) {
		return test(test(x));
	}
}
